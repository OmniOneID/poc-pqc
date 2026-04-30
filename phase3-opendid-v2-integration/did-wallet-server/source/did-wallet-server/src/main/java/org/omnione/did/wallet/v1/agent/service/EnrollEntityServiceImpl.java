/*
 * Copyright 2024 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnione.did.wallet.v1.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.base.datamodel.data.AccEcdh;
import org.omnione.did.base.datamodel.data.Candidate;
import org.omnione.did.base.datamodel.data.DidAuth;
import org.omnione.did.base.datamodel.data.EcdhReqData;
import org.omnione.did.base.datamodel.enums.EccCurveType;
import org.omnione.did.base.datamodel.enums.ProofPurpose;
import org.omnione.did.base.datamodel.enums.SymmetricCipherType;
import org.omnione.did.base.db.constant.WalletServiceStatus;
import org.omnione.did.base.db.domain.CertificateVc;
import org.omnione.did.base.db.domain.WalletServiceInfo;
import org.omnione.did.base.exception.ErrorCode;
import org.omnione.did.base.exception.OpenDidException;
import org.omnione.did.base.property.WalletProperty;
import org.omnione.did.base.utils.*;
import org.omnione.did.common.exception.CommonSdkException;
import org.omnione.did.common.util.DateTimeUtil;
import org.omnione.did.common.util.JsonUtil;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.keypair.EcKeyPair;
import org.omnione.did.crypto.util.MlKemUtils;
import org.omnione.did.data.model.did.DidDocument;
import org.omnione.did.data.model.did.Proof;
import org.omnione.did.data.model.enums.did.ProofType;
import org.omnione.did.data.model.vc.VerifiableCredential;
import org.omnione.did.wallet.v1.admin.service.query.WalletServiceQueryService;
import org.omnione.did.wallet.v1.agent.api.dto.ConfirmEnrollEntityApiReqDto;
import org.omnione.did.wallet.v1.agent.api.dto.ConfirmEnrollEntityApiResDto;
import org.omnione.did.wallet.v1.agent.api.dto.ProposeEnrollEntityApiReqDto;
import org.omnione.did.wallet.v1.agent.api.dto.ProposeEnrollEntityApiResDto;
import org.omnione.did.wallet.v1.agent.api.dto.RequestEcdhApiReqDto;
import org.omnione.did.wallet.v1.agent.api.dto.RequestEcdhApiResDto;
import org.omnione.did.wallet.v1.agent.api.dto.RequestEnrollEntityApiReqDto;
import org.omnione.did.wallet.v1.agent.api.dto.RequestEnrollEntityApiResDto;
import org.omnione.did.wallet.v1.agent.service.query.CertificateVcQueryService;
import org.omnione.did.wallet.v1.agent.api.EnrollFeign;
import org.omnione.did.wallet.v1.agent.dto.EnrollEntityResDto;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of the EnrollEntityService interface.
 * This service is used to enroll entities.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class EnrollEntityServiceImpl implements EnrollEntityService {
    private final EnrollFeign enrollFeign;
    private final CertificateVcQueryService certificateVcQueryService;
    private final DidDocService didDocService;
    private final SignatureService signatureService;
    private final WalletServiceQueryService walletServiceQueryService;
    private final WalletProperty walletProperty;

    /**
     * Enrolls an entity.
     *
     * @return The response DTO.
     * @throws OpenDidException If an error occurs during the enrollment process.
     */
    @Override
    public EnrollEntityResDto enrollEntity() {
        try {
            log.debug("=== Starting enrollEntity ===");

            // Retrieve Wallet Service.
            WalletServiceInfo existedWalletService = walletServiceQueryService.findWalletService();

            // Retrieve Wallet DID Document.
            log.debug("\t--> Retrieving Wallet DID Document");
            DidDocument walletDidDocument = didDocService.getDidDocument(existedWalletService.getDid());

            // Send propose-enroll-entity.
            log.debug("\t--> Send propose-enroll-entity");
            ProposeEnrollEntityApiResDto proposeResponse = proposeEnrollEntity();
            String txId = proposeResponse.getTxId();
            String authNonce = proposeResponse.getAuthNonce();

            // Send request-ecdh
            log.debug("\t--> Send request-ecdh");
            log.debug("\t\t--> generate Tmp KEM Keypair");
            // PQC: ECDH 임시 키쌍 대신 ML-KEM-768 키쌍 생성
            KeyPair kemKeyPair = MlKemUtils.generateKeyPair();
            log.debug("\t\t--> generate ReqEcdh");
            String clientNonce = BaseMultibaseUtil.encode(BaseCryptoUtil.generateNonce(16));
            EcdhReqData reqData = generateReqData(kemKeyPair, clientNonce, walletDidDocument);
            log.debug("\t\t--> request ECDH");
            RequestEcdhApiResDto ecdhResponse = requestEcdh(txId, reqData);

            // Send request-enroll-entity
            log.debug("\t--> Send request-enroll-entity");
            log.debug("\t\t--> generate DID Auth");
            DidAuth didAuth = generateDidAuth(authNonce, walletDidDocument);
            log.debug("\t\t--> request Enroll Entity");
            RequestEnrollEntityApiResDto enrollEntityResponse = requestEnrollEntity(txId, didAuth);
            log.debug("\t\t--> decrypt VC");
            // PQC: ML-KEM 개인키로 복호화 (PrivateKey 타입으로 변경)
            VerifiableCredential vc = decryptVc(kemKeyPair.getPrivate(),
                    ecdhResponse.getAccEcdh(), enrollEntityResponse, clientNonce);

            // Send confirm-enroll-entity
            log.debug("\t--> Send confirm-enroll-entity");
            confirmEnrollEntity(txId, vc.getId());

            // Insert certificate VC.
            log.debug("\t\t--> Insert certificate VC");
            certificateVcQueryService.save(CertificateVc.builder()
                    .vc(vc.toJson())
                    .build());

            log.debug("\t\t--> Update Wallet Service Status");
            existedWalletService.setStatus(WalletServiceStatus.ACTIVATE);
            walletServiceQueryService.save(existedWalletService);
            log.debug("*** Finished enrollEntity ***");

            return EnrollEntityResDto.builder()
                    .build();
        } catch (OpenDidException e) {
            log.error("Error occurred while enrolling entity: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while enrolling entity: {}", e.getMessage());
            throw new OpenDidException(ErrorCode.TR_ENROLL_ENTITY_FAILED);
        }
    }

    /**
     * Enrolls an entity.
     *
     * @return The response DTO.
     * @throws OpenDidException If an error occurs during the enrollment process.
     */
    private ProposeEnrollEntityApiResDto proposeEnrollEntity() {
        ProposeEnrollEntityApiReqDto request = ProposeEnrollEntityApiReqDto.builder()
                .id(RandomUtil.generateMessageId())
                .build();
        return enrollFeign.proposeEnrollEntityApi(request);
    }

    /**
     * Enrolls an entity.
     *
     * @return The response DTO.
     * @throws OpenDidException If an error occurs during the enrollment process.
     */
    private RequestEcdhApiResDto requestEcdh(String txId, EcdhReqData reqEcdh) {
        RequestEcdhApiReqDto request = RequestEcdhApiReqDto.builder()
                .id(RandomUtil.generateMessageId())
                .txId(txId)
                .reqEcdh(reqEcdh)
                .build();
        try {
            log.debug("Request ECDH: ");
            log.debug(JsonUtil.serializeToJson(request));
        } catch (CommonSdkException e) {
            log.error("Error occurred while serializing request: {}", e.getMessage());
            throw new OpenDidException(ErrorCode.JSON_PROCESSING_ERROR);
        }
        return enrollFeign.requestEcdh(request);
    }

    /**
     * Generates an ECDH request data object.
     *
     * @param kemKeyPair The ML-KEM-768 key pair (public key sent to server).
     * @param clientNonce The client nonce.
     * @param walletDidDocument The wallet DID document.
     * @return The ECDH request data object.
     */
    private EcdhReqData generateReqData(KeyPair kemKeyPair, String clientNonce, DidDocument walletDidDocument) {
        // PQC: ML-KEM-768 및 Secp256r1 동시 지원 협상 목록 포함
        Candidate candidate = Candidate.builder()
                .ciphers(Arrays.asList(SymmetricCipherType.values()))
                .keyAgreements(List.of("ML-KEM-768", "Secp256r1"))
                .build();

        // keyagree key is always SECP256R1 regardless of the global key algorithm setting
        Proof proof = new Proof();
        proof.setType(ProofType.SECP256R1_SIGNATURE_2018.getRawValue());
        proof.setProofPurpose(org.omnione.did.base.datamodel.enums.ProofPurpose.KEY_AGREEMENT.toString());
        proof.setCreated(DateTimeUtil.getCurrentUTCTimeString());
        proof.setVerificationMethod(signatureService.getVerificationMethod(walletDidDocument, org.omnione.did.base.datamodel.enums.ProofPurpose.KEY_AGREEMENT));

        // PQC: curve 대신 algorithm 필드로 ML-KEM-768 명시, X.509 인코딩 후 multibase 인코딩
        EcdhReqData signatureObject = EcdhReqData.builder()
                .client(walletDidDocument.getId())
                .clientNonce(clientNonce)
                .algorithm("ML-KEM-768")
                .publicKey(BaseMultibaseUtil.encode(kemKeyPair.getPublic().getEncoded()))
                .candidate(candidate)
                .proof(proof)
                .build();

        return signatureService.signEcdhReq(walletDidDocument, signatureObject);
    }

    /**
     * Requests the enrollment of an entity.
     *
     * @return The response DTO.
     * @throws OpenDidException If an error occurs during the enrollment process.
     */
    private RequestEnrollEntityApiResDto requestEnrollEntity(String txId, DidAuth didAuth) {
        RequestEnrollEntityApiReqDto request = RequestEnrollEntityApiReqDto.builder()
                .id(RandomUtil.generateMessageId())
                .txId(txId)
                .didAuth(didAuth)
                .build();
        return enrollFeign.requestEnrollEntityApi(request);
    }

    /**
     * Generates a DID Auth object.
     *
     * @param authNonce The auth nonce.
     * @param walletDidDocument The wallet DID document.
     * @return The DID Auth object.
     */
    private DidAuth generateDidAuth(String authNonce, DidDocument walletDidDocument) {
        // Generate Proof object.
        // Derive proof type from the actual key type in the DID document, not from the global config.
        // This handles cases where the wallet file was initialized with a different algorithm.
        String authKeyType = BaseCoreDidUtil.getVerificationMethod(walletDidDocument, "auth").getType();
        Proof proof = new Proof();
        proof.setType(resolveProofTypeFromKeyType(authKeyType));
        proof.setProofPurpose(org.omnione.did.base.datamodel.enums.ProofPurpose.AUTHENTICATION.toString());
        proof.setCreated(DateTimeUtil.getCurrentUTCTimeString());
        proof.setVerificationMethod(signatureService.getVerificationMethod(walletDidDocument, ProofPurpose.AUTHENTICATION));

        // Generate Signature Object. (except ProofValue)
        DidAuth signatureObject = DidAuth.builder()
                .authNonce(authNonce)
                .did(walletDidDocument.getId())
                .proof(proof)
                .build();

        return signatureService.signDidAuth(walletDidDocument, signatureObject);
    }

    /**
     * Decrypts a verifiable credential.
     *
     * @param privateKey The private key (ML-KEM or EC).
     * @param accEcdh The ECDH object.
     * @param enrollEntityResponse The enrollment response.
     * @param clientNonce The client nonce.
     * @return The decrypted verifiable credential.
     */
    private VerifiableCredential decryptVc(PrivateKey privateKey, AccEcdh accEcdh, RequestEnrollEntityApiResDto enrollEntityResponse, String clientNonce) {
        try {
            byte[] sharedSecret;
            // PQC: 서버가 선택한 알고리즘에 따라 세션 키 도출 방식 분기
            if ("ML-KEM-768".equals(accEcdh.getSelectedAlgorithm())) {
                byte[] ciphertext = BaseMultibaseUtil.decode(accEcdh.getCiphertext());
                sharedSecret = MlKemUtils.decapsulate(privateKey, ciphertext);
            } else {
                byte[] compressedPublicKey = BaseMultibaseUtil.decode(accEcdh.getPublicKey());
                sharedSecret = BaseCryptoUtil.generateSharedSecret(compressedPublicKey, ((ECPrivateKey) privateKey).getEncoded(), EccCurveType.SECP_256_R1);
            }

            byte[] mergeNonce = BaseCryptoUtil.mergeNonce(clientNonce, accEcdh.getServerNonce());
            byte[] sessionKey = BaseCryptoUtil.mergeSharedSecretAndNonce(sharedSecret, mergeNonce, accEcdh.getCipher());

            byte[] iv = BaseMultibaseUtil.decode(enrollEntityResponse.getIv());
            byte[] decrypt = BaseCryptoUtil.decrypt(
                    enrollEntityResponse.getEncVc(),
                    sessionKey,
                    iv,
                    accEcdh.getCipher(),
                    accEcdh.getPadding()
            );

            String jsonVc = new String(decrypt);
            VerifiableCredential vc = new VerifiableCredential();
            vc.fromJson(jsonVc);
            return vc;

        } catch (CryptoException e) {
            log.error("ML-KEM decapsulate failed: {}", e.getMessage(), e);
            throw new OpenDidException(ErrorCode.TR_ENROLL_ENTITY_FAILED);
        }
    }

    // PQC: 알고리즘 설정에 따라 proof 타입 반환
    private String resolveProofType(String keyAlgorithm) {
        if ("MlDsa44".equals(keyAlgorithm)) {
            return ProofType.ML_DSA_44_SIGNATURE_2024.getRawValue();
        }
        return ProofType.SECP256R1_SIGNATURE_2018.getRawValue();
    }

    // DID 문서의 실제 키 타입으로 proof 타입 결정 (wallet 설정과 실제 키 알고리즘 불일치 방지)
    private String resolveProofTypeFromKeyType(String keyType) {
        if ("Mldsa44VerificationKey2024".equals(keyType) || "MlDsa44VerificationKey2024".equals(keyType)) {
            return ProofType.ML_DSA_44_SIGNATURE_2024.getRawValue();
        }
        return ProofType.SECP256R1_SIGNATURE_2018.getRawValue();
    }

    /**
     * Confirms the enrollment of an entity.
     *
     * @param txId The transaction ID.
     * @param vcId The VC ID.
     * @return The response DTO.
     * @throws OpenDidException If an error occurs during the enrollment process.
     */
    private ConfirmEnrollEntityApiResDto confirmEnrollEntity(String txId, String vcId) {
        ConfirmEnrollEntityApiReqDto request = ConfirmEnrollEntityApiReqDto.builder()
                .id(RandomUtil.generateMessageId())
                .txId(txId)
                .vcId(vcId)
                .build();

        return enrollFeign.confirmEnrollEntityApi(request);
    }
}
