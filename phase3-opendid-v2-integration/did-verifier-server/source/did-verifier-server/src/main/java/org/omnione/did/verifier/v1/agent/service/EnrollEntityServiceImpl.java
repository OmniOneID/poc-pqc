/*
 * Copyright 2025 OmniOne.
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

package org.omnione.did.verifier.v1.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.base.datamodel.data.AccEcdh;
import org.omnione.did.base.datamodel.data.Candidate;
import org.omnione.did.base.datamodel.data.DidAuth;
import org.omnione.did.base.datamodel.data.EcdhReqData;
import org.omnione.did.base.datamodel.enums.EccCurveType;
import org.omnione.did.base.datamodel.enums.SymmetricCipherType;
import org.omnione.did.base.db.constant.VerifierStatus;
import org.omnione.did.base.db.domain.CertificateVc;
import org.omnione.did.base.db.domain.VerifierInfo;
import org.omnione.did.base.exception.ErrorCode;
import org.omnione.did.base.exception.OpenDidException;
import org.omnione.did.base.property.WalletProperty;
import org.omnione.did.base.util.*;
import org.omnione.did.common.exception.CommonSdkException;
import org.omnione.did.common.util.JsonUtil;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.keypair.EcKeyPair;
import org.omnione.did.crypto.util.MlKemUtils;
import org.omnione.did.data.model.did.DidDocument;
import org.omnione.did.data.model.did.Proof;
import org.omnione.did.data.model.enums.did.ProofPurpose;
import org.omnione.did.data.model.enums.did.ProofType;
import org.omnione.did.data.model.vc.VerifiableCredential;
import org.omnione.did.verifier.v1.admin.service.VerifierInfoQueryService;
import org.omnione.did.verifier.v1.agent.api.EnrollFeign;
import org.omnione.did.verifier.v1.agent.api.dto.*;
import org.omnione.did.verifier.v1.agent.dto.EnrollEntityResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.Arrays;
import java.util.List;

/**
 * The EnrollEntityServiceImpl class manages the entity enrollment process.
 * This service coordinates multiple steps including proposing enrollment,
 * performing ECDH key exchange, requesting enrollment, and confirming enrollment.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class EnrollEntityServiceImpl implements EnrollEntityService {
    private final EnrollFeign enrollFeign;
    private final FileWalletService walletManagerInterface;
    private final CertificateVcQueryService certificateVcQueryService;
    private final VerifierInfoQueryService verifierInfoQueryService;
    private final DidDocService didDocService;
    private final WalletProperty walletProperty; // PQC:

    /**
     * Enrolls an entity through a multistep process:
     * 1. Proposes entity enrollment
     * 2. Performs ECDH key exchange
     * 3. Requests entity enrollment
     * 4. Confirms entity enrollment
     *
     * This method orchestrates the entire enrollment flow, handling cryptographic
     * operations, API calls, and data persistence.
     *
     * @return EnrollEntityResDto An empty response indicating successful enrollment.
     */
    @Override
    public EnrollEntityResDto enrollEntity() {

        log.debug("=== Starting Enroll Entity ===");
        VerifierInfo verifierInfo = verifierInfoQueryService.getVerifierInfo();
        DidDocument verifierDidDoc = didDocService.getDidDocument(verifierInfo.getDid());

        log.debug("\t--> 1. propose Enroll Entity");
        ProposeEnrollEntityApiResDto proposeResponse = proposeEnrollEntity();
        String txId = proposeResponse.getTxId();
        String authNonce = proposeResponse.getAuthNonce();

        log.debug("\t--> 2. request ECDH");
        log.debug("\t\t--> generate Tmp KEM Keypair");
        // PQC: ECDH 임시 키쌍 대신 ML-KEM-768 키쌍 생성
        KeyPair kemKeyPair;
        try {
            kemKeyPair = MlKemUtils.generateKeyPair();
        } catch (CryptoException e) {
            throw new OpenDidException(ErrorCode.CRYPTO_ERROR);
        }
        log.debug("\t\t--> generate ReqEcdh");
        String clientNonce = BaseMultibaseUtil.encode(BaseCryptoUtil.generateNonce(16));
        EcdhReqData reqData = generateReqData(kemKeyPair, clientNonce);
        log.debug("\t\t--> request ECDH");
        RequestEcdhApiResDto ecdhResponse = requestEcdh(txId, reqData);

        log.debug("\t--> 3. request Enroll Entity");
        log.debug("\t\t--> generate DID Auth");
        DidAuth didAuth = generateDidAuth(authNonce);
        log.debug("\t\t--> request Enroll Entity");
        RequestEnrollEntityApiResDto enrollEntityResponse = requestEnrollEntity(txId, didAuth);
        log.debug("\t\t--> decrypt VC");
        // PQC: ML-KEM 개인키로 복호화 (PrivateKey 타입으로 변경)
        VerifiableCredential vc = decryptVc(kemKeyPair.getPrivate(),
                ecdhResponse.getAccEcdh(), enrollEntityResponse, clientNonce);

        log.debug("\t--> 4. confirm Enroll Entity");
        ConfirmEnrollEntityApiResDto confirmResponse = confirmEnrollEntity(txId, vc.getId());

        log.debug("\t--> 5. VerifierInfo Status Update");

        verifierInfo.setStatus(VerifierStatus.ACTIVATE);
        verifierInfoQueryService.save(verifierInfo);


        log.debug("\t\t--> save VC to DB");
        certificateVcQueryService.save(CertificateVc.builder()
                .vc(vc.toJson())
                .build());

        log.debug("== Finish");
        return EnrollEntityResDto.builder()
                .build();

    }

    /**
     * Initiates the enrollment process by sending a proposal to the enrollment service.
     * This is the first step in the enrollment flow.
     *
     * @return ProposeEnrollEntityApiResDto containing transaction ID and authentication nonce
     */
    private ProposeEnrollEntityApiResDto proposeEnrollEntity() {
        ProposeEnrollEntityApiReqDto request = ProposeEnrollEntityApiReqDto.builder()
                .id(RandomUtil.generateMessageId())
                .build();
        return enrollFeign.proposeEnrollEntityApi(request);
    }

    /**
     * Requests ECDH (Elliptic Curve Diffie-Hellman) key exchange.
     * This step is crucial for establishing a secure communication channel.
     *
     * @param txId    The transaction ID obtained from the proposal step
     * @param reqEcdh The ECDH request data containing necessary cryptographic information
     * @return RequestEcdhApiResDto containing the server's response to the ECDH request
     */
    private RequestEcdhApiResDto requestEcdh(String txId, EcdhReqData reqEcdh) {
        RequestEcdhApiReqDto request = RequestEcdhApiReqDto.builder()
                .id(RandomUtil.generateMessageId())
                .txId(txId)
                .reqEcdh(reqEcdh)
                .build();
        return enrollFeign.requestEcdh(request);
    }

    /**
     * Generates the ECDH request data, including public key, nonce, and proof.
     * This data is essential for the key exchange process.
     *
     * @param kemKeyPair  The ML-KEM-768 key pair (public key sent to server)
     * @param clientNonce A client-generated nonce for added security
     * @return EcdhReqData containing all necessary ECDH request information
     * @throws OpenDidException if cryptographic operations fail
     */
    private EcdhReqData generateReqData(KeyPair kemKeyPair, String clientNonce) {
        // PQC: ML-KEM-768 및 Secp256r1 동시 지원 협상 목록 포함
        Candidate candidate = Candidate.builder()
                .ciphers(Arrays.asList(SymmetricCipherType.values()))
                .keyAgreements(List.of("ML-KEM-768", "Secp256r1"))
                .build();

        String verificationMethod = "did:omn:verifier?versionId=1#keyagree";
        Proof proof = BaseCryptoUtil.generateProof(resolveProofTypeEnum("keyagree"), // PQC:
                ProofPurpose.KEY_AGREEMENT, verificationMethod);

        EcdhReqData reqData = EcdhReqData.builder()
                .client("did:omn:verifier")
                .clientNonce(clientNonce)
                // PQC: curve 대신 algorithm 필드로 ML-KEM-768 명시
                .algorithm("ML-KEM-768")
                // PQC: ML-KEM 공개키는 X.509 인코딩 후 multibase 인코딩
                .publicKey(BaseMultibaseUtil.encode(kemKeyPair.getPublic().getEncoded()))
                .candidate(candidate)
                .proof(proof)
                .build();

        proof.setProofValue(signData(reqData, "keyagree"));

        return reqData;
    }

    /**
     * Sends the actual enrollment request to the enrollment service.
     * This step uses the DID authentication generated from the previous steps.
     *
     * @param txId    The transaction ID for this enrollment process
     * @param didAuth The DID authentication object
     * @return RequestEnrollEntityApiResDto containing the enrollment response
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
     * Generates the DID authentication object required for the enrollment request.
     * This includes creating a proof of authentication.
     *
     * @param authNonce The authentication nonce received in the proposal step
     * @return DidAuth object containing the authentication proof
     */
    private DidAuth generateDidAuth(String authNonce) {

        String verificationMethod = "did:omn:verifier?versionId=1#auth";

        Proof proof = BaseCryptoUtil.generateProof(resolveProofTypeEnum("auth"), // PQC:
                ProofPurpose.AUTHENTICATION, verificationMethod);

        DidAuth didAuth = DidAuth.builder()
                .authNonce(authNonce)
                .did("did:omn:verifier")
                .proof(proof)
                .build();

        proof.setProofValue(signData(didAuth, "auth"));
        return didAuth;
    }


    /**
     * Decrypts the Verifiable Credential (VC) received in the enrollment response.
     * This method uses the ML-KEM shared secret (or ECDH fallback) and other cryptographic
     * parameters to securely decrypt the VC.
     *
     * @param privateKey           The private key used for decryption (ML-KEM or EC)
     * @param accEcdh              The accepted ECDH parameters from the server
     * @param enrollEntityResponse The enrollment response containing the encrypted VC
     * @param clientNonce          The client nonce used in the ECDH process
     * @return VerifiableCredential The decrypted and parsed VC
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
            throw new OpenDidException(ErrorCode.CRYPTO_ERROR);
        }
    }

    /**
     * Confirms the entity enrollment by sending a confirmation request to the enrollment service.
     * This is the final step in the enrollment process.
     *
     * @param txId The transaction ID for this enrollment process
     * @param vcId The ID of the Verifiable Credential received
     * @return ConfirmEnrollEntityApiResDto containing the confirmation response
     */
    private ConfirmEnrollEntityApiResDto confirmEnrollEntity(String txId, String vcId) {
        ConfirmEnrollEntityApiReqDto request = ConfirmEnrollEntityApiReqDto.builder()
                .id(RandomUtil.generateMessageId())
                .txId(txId)
                .vcId(vcId)
                .build();

        return enrollFeign.confirmEnrollEntityApi(request);
    }

    // PQC: 알고리즘 설정에 따라 ProofType enum 반환
    private ProofType resolveProofTypeEnum(String keyId) {
        if ("keyagree".equals(keyId)) {
            return ProofType.SECP256R1_SIGNATURE_2018;
        }
        if ("MlDsa44".equals(walletProperty.getKeyAlgorithm())) {
            return ProofType.ML_DSA_44_SIGNATURE_2024;
        }
        return ProofType.SECP256R1_SIGNATURE_2018;
    }

    /**
     * Signs the given data using the specified key ID.
     * This method is used to generate signatures for various parts of the enrollment process.
     *
     * @param source The data to be signed
     * @param keyId  The ID of the key to use for signing
     * @return String The base58-encoded signature
     * @throws OpenDidException if JSON serialization fails
     */
    private String signData(Object source, String keyId) {
        try {
            String serializeSource = JsonUtil.serializeAndSort(source);
            byte[] signature =  walletManagerInterface.generateCompactSignature(keyId, serializeSource);
            return BaseMultibaseUtil.encode(signature);
        } catch (CommonSdkException e) {
            throw new OpenDidException(ErrorCode.JSON_PARSE_ERROR);
        }
    }
}
