/*
 * Copyright 2024 - 2025 OmniOne.
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

package org.omnione.did.issuer.v1.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;
import org.omnione.did.base.constants.UrlConstant;
import org.omnione.did.base.datamodel.data.AccEcdh;
import org.omnione.did.base.datamodel.data.Candidate;
import org.omnione.did.base.datamodel.data.DidAuth;
import org.omnione.did.base.datamodel.data.EcdhReqData;
import org.omnione.did.base.datamodel.enums.EccCurveType;
import org.omnione.did.base.datamodel.enums.SymmetricCipherType;
import org.omnione.did.base.db.constant.IssuerStatus;
import org.omnione.did.base.db.domain.CertificateVc;
import org.omnione.did.base.db.domain.IssuerInfo;
import org.omnione.did.base.exception.ErrorCode;
import org.omnione.did.base.exception.OpenDidException;
import org.omnione.did.base.response.ErrorResponse;
import org.omnione.did.base.util.BaseCryptoUtil;
import org.omnione.did.base.util.BaseMultibaseUtil;
import org.omnione.did.base.util.RandomUtil;
import org.omnione.did.common.exception.HttpClientException;
import org.omnione.did.common.util.HttpClientUtil;
import org.omnione.did.common.util.JsonUtil;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.keypair.EcKeyPair;
import org.omnione.did.crypto.util.MlKemUtils;
import org.omnione.did.data.model.did.DidDocument;
import org.omnione.did.data.model.did.Proof;
import org.omnione.did.data.model.enums.did.ProofPurpose;
import org.omnione.did.data.model.enums.did.ProofType;
import org.omnione.did.data.model.vc.VerifiableCredential;
import org.omnione.did.base.property.WalletProperty;
import org.omnione.did.issuer.v1.admin.service.query.ApplicationConfigQueryService;
import org.omnione.did.issuer.v1.agent.api.dto.*;
import org.omnione.did.issuer.v1.agent.service.query.CertificateVcQueryService;

import org.omnione.did.issuer.v1.agent.dto.EnrollEntityResDto;
import org.omnione.did.issuer.v1.agent.service.query.IssuerInfoQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.Arrays;
import java.util.List;

/**
 * Service for enrolling entity.
 * This class provides methods for enrolling entity.
 */
@Slf4j
@Profile("!sample")
@Service
@Transactional
public class EnrollEntityServiceImpl implements EnrollEntityService {
    private final FileWalletService walletService;
    private final CertificateVcQueryService certificateVcQueryService;
    private final DidDocService didDocService;
    private final IssuerInfoQueryService issuerInfoQueryService;
    private final SignatureService signatureService;
    private final WalletProperty walletProperty; // PQC:
    @Value(value = "${tas.url}")
    private String TAS_URL;

    public EnrollEntityServiceImpl(FileWalletService walletService, CertificateVcQueryService certificateVcQueryService,
                                   IssuerInfoQueryService issuerInfoQueryService, DidDocService didDocService,
                                   SignatureService signatureService, ApplicationConfigQueryService applicationConfigQueryService,
                                   WalletProperty walletProperty) {
        this.walletService = walletService;
        this.certificateVcQueryService = certificateVcQueryService;
        this.issuerInfoQueryService = issuerInfoQueryService;
        this.didDocService = didDocService;
        this.signatureService = signatureService;
        this.walletProperty = walletProperty; // PQC:
//        this.TAS_URL = applicationConfigQueryService.getApplicationConfig().getTasUrl();
    }

    /**
     * Enroll entity.
     * This method enrolls an entity with the TAS and returns the result as an EnrollEntityResDto object.
     *
     * @return the result of the entity enrollment
     * @throws OpenDidException if the entity enrollment fails
     */
    public EnrollEntityResDto enrollEntity() {
        try {
            log.debug("*** Start enrollEntity ***");

            IssuerInfo issuerInfo = issuerInfoQueryService.findIssuerInfo();
            DidDocument issuerDidDocument = didDocService.getDidDocument(issuerInfo.getDid());

            log.debug("\t--> 1. propose Enroll Entity");
            ProposeEnrollEntityApiResDto proposeResponse = proposeEnrollEntity();
            String txId = proposeResponse.getTxId();
            String authNonce = proposeResponse.getAuthNonce();

            log.debug("\t--> 2. request ECDH");
            log.debug("\t\t--> generate Tmp Keypair (ML-KEM-768)");
            // PQC: EC 임시 키쌍 대신 ML-KEM 임시 키쌍 생성
            KeyPair kemKeyPair = MlKemUtils.generateKeyPair();
            log.debug("\t\t--> generate ReqEcdh");
            String clientNonce = BaseMultibaseUtil.encode(BaseCryptoUtil.generateNonce(16));
            EcdhReqData reqData = generateReqData(kemKeyPair, clientNonce, issuerDidDocument);
            log.debug("\t\t--> request ECDH");
            RequestEcdhApiResDto ecdhResponse = requestEcdh(txId, reqData);

            log.debug("\t--> 3. request Enroll Entity");
            log.debug("\t\t--> generate DID Auth");
            DidAuth didAuth = generateDidAuth(authNonce, issuerDidDocument);
            log.debug("\t\t--> request Enroll Entity");
            RequestEnrollEntityApiResDto enrollEntityResponse = requestEnrollEntity(txId, didAuth);
            log.debug("\t\t--> decrypt VC");
            // PQC: ML-KEM 개인키로 decapsulate하여 세션키 도출
            VerifiableCredential vc = decryptVc(kemKeyPair.getPrivate(),
                    ecdhResponse.getAccEcdh(), enrollEntityResponse, clientNonce);

            log.debug("\t--> 4. confirm Enroll Entity");
            ConfirmEnrollEntityApiResDto confirmResponse = confirmEnrollEntity(txId, vc.getId());

            log.debug("\t\t--> save VC to DB");
            certificateVcQueryService.save(CertificateVc.builder()
                    .vc(vc.toJson())
                    .build());

            log.debug("\t\t--> Update Issuer Status");
            issuerInfo.setStatus(IssuerStatus.ACTIVATE);
            issuerInfoQueryService.save(issuerInfo);

            log.debug("*** Finished enrollEntity ***");

            return EnrollEntityResDto.builder()
                    .build();
        } catch(OpenDidException e) {
            log.error("OpenDidException occurred during enrollEntity: {}", e.getErrorCode().getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Exception occurred during enrollEntity: {}", e.getMessage(), e);
            throw new OpenDidException(ErrorCode.TR_ENROLL_ENTITY_FAILED);
        }
    }

    /**
     * Propose enroll entity.
     * This method sends a propose-enroll-entity request to the TAS and returns the response as a ProposeEnrollEntityApiResDto object.
     *
     * @return the response to the propose-enroll-entity request
     */
    private ProposeEnrollEntityApiResDto proposeEnrollEntity() {
        ProposeEnrollEntityApiReqDto request = ProposeEnrollEntityApiReqDto.builder()
                .id(RandomUtil.generateMessageId())
                .build();

        String url = TAS_URL + UrlConstant.Tas.V1 + UrlConstant.Tas.PROPOSE_ENROLL_ENTITY;
        try {
            return HttpClientUtil.postData(url, JsonUtil.serializeToJson(request), ProposeEnrollEntityApiResDto.class);
        } catch (HttpClientException e) {
            log.error("HttpClientException occurred while sending generate-profile request: {}", e.getResponseBody(), e);
            ErrorResponse errorResponse = convertExternalErrorResponse(e.getResponseBody());
            throw new OpenDidException(errorResponse);
        }
    }

    /**
     * Request ECDH.
     * This method sends a request-ecdh request to the TAS and returns the response as a RequestEcdhApiResDto object.
     *
     * @param txId the transaction ID
     * @param reqEcdh the ECDH request data
     * @return the response to the request-ecdh request
     */
    private RequestEcdhApiResDto requestEcdh(String txId, EcdhReqData reqEcdh) {
        RequestEcdhApiReqDto request = RequestEcdhApiReqDto.builder()
                .id(RandomUtil.generateMessageId())
                .txId(txId)
                .reqEcdh(reqEcdh)
                .build();


        String url = TAS_URL + UrlConstant.Tas.V1 + UrlConstant.Tas.REQUEST_ECDH;
        try {
            return HttpClientUtil.postData(url, JsonUtil.serializeToJson(request), RequestEcdhApiResDto.class);
        } catch (HttpClientException e) {
            log.error("HttpClientException occurred while sending generate-profile request: {}", e.getResponseBody(), e);
            ErrorResponse errorResponse = convertExternalErrorResponse(e.getResponseBody());
            throw new OpenDidException(errorResponse);
        }
    }
    /**
     * Generate request data.
     * This method generates the request data for the ECDH request.
     *
     * @param publicKey the public key
     * @param curveType the ECC curve type
     * @param clientNonce the client nonce
     * @param issuerDidDocument the Issuer DID document
     * @return the generated request data
     * @throws OpenDidException if the public key compression fails
     */
    // PQC: ML-KEM 임시 키쌍으로 reqEcdh 생성
    // - algorithm: "ML-KEM-768" (curve 대신)
    // - publicKey: ML-KEM 임시 공개키 (X.509, 1,184B)
    // - candidate.keyAgreements: ["ML-KEM-768", "Secp256r1"] (서버 협상용)
    // - proof: keyagree(Secp256r1)로 서명 — 그대로 유지 (옵션 A)
    private EcdhReqData generateReqData(KeyPair kemKeyPair, String clientNonce, DidDocument issuerDidDocument) {
        Candidate candidate = Candidate.builder()
                .ciphers(Arrays.asList(SymmetricCipherType.values()))
                .keyAgreements(List.of("ML-KEM-768", "Secp256r1"))
                .build();

        Proof proof = BaseCryptoUtil.generateProof(ProofType.SECP256R1_SIGNATURE_2018,
                ProofPurpose.KEY_AGREEMENT, signatureService.getVerificationMethod(issuerDidDocument, org.omnione.did.base.datamodel.enums.ProofPurpose.KEY_AGREEMENT));

        // ML-KEM 공개키를 X.509 인코딩 후 multibase로 직렬화
        String encodedKemPubKey = BaseMultibaseUtil.encode(kemKeyPair.getPublic().getEncoded());

        EcdhReqData reqData = EcdhReqData.builder()
                .client("did:omn:issuer")
                .clientNonce(clientNonce)
                .algorithm("ML-KEM-768")
                .publicKey(encodedKemPubKey)
                .candidate(candidate)
                .proof(proof)
                .build();

        proof.setProofValue(signData(reqData, "keyagree"));

        return reqData;
    }

    /**
     * Request enroll entity.
     * This method sends a request-enroll-entity request to the TAS and returns the response as a RequestEnrollEntityApiResDto object.
     *
     * @param txId the transaction ID
     * @param didAuth the DID Auth object
     * @return the response to the request-enroll-entity request
     */
    private RequestEnrollEntityApiResDto requestEnrollEntity(String txId, DidAuth didAuth) {
        RequestEnrollEntityApiReqDto request = RequestEnrollEntityApiReqDto.builder()
                .id(RandomUtil.generateMessageId())
                .txId(txId)
                .didAuth(didAuth)
                .build();

        String url = TAS_URL + UrlConstant.Tas.V1 + UrlConstant.Tas.REQUEST_ENROLL_ENTITY;
        try {
            return HttpClientUtil.postData(url, JsonUtil.serializeToJson(request), RequestEnrollEntityApiResDto.class);
        } catch (HttpClientException e) {
            log.error("HttpClientException occurred while sending generate-profile request: {}", e.getResponseBody(), e);
            ErrorResponse errorResponse = convertExternalErrorResponse(e.getResponseBody());
            throw new OpenDidException(errorResponse);
        }
    }

    /**
     * Generate DID Auth.
     * This method generates the DID Auth object for the request-enroll-entity request.
     *
     * @param authNonce the authentication nonce
     * @param issuerDidDocument the Issuer DID document
     * @return the generated DID Auth object
     */
    private DidAuth generateDidAuth(String authNonce, DidDocument issuerDidDocument) {

        String verificationMethod = "did:omn:issuer?versionId=1#auth";

        Proof proof = BaseCryptoUtil.generateProof(resolveProofTypeEnum(walletProperty.getKeyAlgorithm()), // PQC:
                ProofPurpose.AUTHENTICATION, signatureService.getVerificationMethod(issuerDidDocument, org.omnione.did.base.datamodel.enums.ProofPurpose.AUTHENTICATION));

        DidAuth didAuth = DidAuth.builder()
                .authNonce(authNonce)
                .did(issuerDidDocument.getId())
                .proof(proof)
                .build();

        return signatureService.signDidAuth(issuerDidDocument, didAuth);
    }

    /**
     * Decrypt VC.
     * This method decrypts the Verifiable Credential received from the TAS.
     *
     * @param privateKey the private key
     * @param accEcdh the account ECDH data
     * @param enrollEntityResponse the response to the request-enroll-entity request
     * @param clientNonce the client nonce
     * @return the decrypted Verifiable Credential
     */
    // PQC: selectedAlgorithm 확인 후 ML-KEM 또는 ECDH 경로로 분기
    private VerifiableCredential decryptVc(PrivateKey privateKey, AccEcdh accEcdh,
            RequestEnrollEntityApiResDto enrollEntityResponse, String clientNonce) {
        try {
            byte[] sharedSecret;
            if ("ML-KEM-768".equals(accEcdh.getSelectedAlgorithm())) {
                // PQC: ciphertext를 ML-KEM 개인키로 decapsulate하여 sharedSecret 도출
                byte[] ciphertext = BaseMultibaseUtil.decode(accEcdh.getCiphertext());
                sharedSecret = MlKemUtils.decapsulate(privateKey, ciphertext);
            } else {
                // 기존 ECDH 경로
                byte[] compressedPublicKey = BaseMultibaseUtil.decode(accEcdh.getPublicKey());
                sharedSecret = BaseCryptoUtil.generateSharedSecret(
                        compressedPublicKey, ((ECPrivateKey) privateKey).getEncoded(), EccCurveType.SECP_256_R1);
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
            throw new OpenDidException(ErrorCode.CRYPTO_SESSION_KEY_GENERATION_FAILED);
        }
    }

    /**
     * Confirm enroll entity.
     * This method sends a confirm-enroll-entity request to the TAS and returns the response as a ConfirmEnrollEntityApiResDto object.
     *
     * @param txId the transaction ID
     * @param vcId the Verifiable Credential ID
     * @return the response to the confirm-enroll-entity request
     */
    private ConfirmEnrollEntityApiResDto confirmEnrollEntity(String txId, String vcId) {
        ConfirmEnrollEntityApiReqDto request = ConfirmEnrollEntityApiReqDto.builder()
                .id(RandomUtil.generateMessageId())
                .txId(txId)
                .vcId(vcId)
                .build();


        String url = TAS_URL + UrlConstant.Tas.V1 + UrlConstant.Tas.CONFIRM_ENROLL_ENTITY;
        try {
            return HttpClientUtil.postData(url, JsonUtil.serializeToJson(request), ConfirmEnrollEntityApiResDto.class);
        } catch (HttpClientException e) {
            log.error("HttpClientException occurred while sending generate-profile request: {}", e.getResponseBody(), e);
            ErrorResponse errorResponse = convertExternalErrorResponse(e.getResponseBody());
            throw new OpenDidException(errorResponse);
        }
    }

    /**
     * Sign data.
     * This method signs the data using the specified key ID.
     *
     * @param source the data to sign
     * @param keyId the key ID
     * @return the signature
     */
    private String signData(Object source, String keyId) {
        String serializeSource = JsonUtil.serializeAndSort(source);
        byte[] signature = walletService.generateCompactSignature(keyId, serializeSource);

        return BaseMultibaseUtil.encode(signature);
    }

    // PQC: 알고리즘 설정에 따라 ProofType enum 반환
    private ProofType resolveProofTypeEnum(String keyAlgorithm) {
        if ("MlDsa44".equals(keyAlgorithm)) {
            return ProofType.ML_DSA_44_SIGNATURE_2024;
        }
        return ProofType.SECP256R1_SIGNATURE_2018;
    }

    /**
     * Converts an external error response string to an ErrorResponse object.
     * This method attempts to parse the given JSON string into an ErrorResponse instance.
     *
     * @param resBody The JSON string representing the external error response
     * @return An ErrorResponse object parsed from the input string
     * @throws OpenDidException with ErrorCode.ISSUER_UNKNOWN_RESPONSE if parsing fails
     */
    private ErrorResponse convertExternalErrorResponse(String resBody) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(resBody, ErrorResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse external error response: {}", resBody, e);
            throw new OpenDidException(ErrorCode.TAS_UNKNOWN_RESPONSE);
        }
    }
}
