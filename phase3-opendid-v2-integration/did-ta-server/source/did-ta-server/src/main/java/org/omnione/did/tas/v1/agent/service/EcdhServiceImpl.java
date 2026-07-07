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

package org.omnione.did.tas.v1.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.omnione.did.base.datamodel.data.AccEcdh;
import org.omnione.did.base.datamodel.data.Candidate;
import org.omnione.did.base.datamodel.data.EcdhReqData;
import org.omnione.did.base.datamodel.data.Proof;
import org.omnione.did.base.datamodel.enums.EccCurveType;
import org.omnione.did.base.datamodel.enums.ProofPurpose;
import org.omnione.did.base.datamodel.enums.ProofType;
import org.omnione.did.base.datamodel.enums.SymmetricCipherType;
import org.omnione.did.base.datamodel.enums.SymmetricPaddingType;
import org.omnione.did.base.db.constant.SubTransactionStatus;
import org.omnione.did.base.db.constant.SubTransactionType;
import org.omnione.did.base.db.constant.TransactionStatus;
import org.omnione.did.base.db.domain.Ecdh;
import org.omnione.did.base.db.domain.SubTransaction;
import org.omnione.did.base.db.domain.Tas;
import org.omnione.did.base.db.domain.Transaction;
import org.omnione.did.base.db.repository.EcdhRepository;
import org.omnione.did.base.exception.ErrorCode;
import org.omnione.did.base.exception.OpenDidException;
import org.omnione.did.base.util.BaseCoreDidUtil;
import org.omnione.did.base.util.BaseCryptoUtil;
import org.omnione.did.base.util.BaseDigestUtil;
import org.omnione.did.base.util.BaseMultibaseUtil;
import org.omnione.did.common.exception.CommonSdkException;
import org.omnione.did.tas.v1.agent.dto.entity.RequestECDHReqDto;
import org.omnione.did.tas.v1.agent.dto.entity.RequestECDHResDto;
import org.omnione.did.tas.v1.agent.dto.entity.TestRequestEcdhOnlyReqDto;
import org.omnione.did.tas.v1.agent.dto.entity.TestRequestEcdhOnlyResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.common.util.DateTimeUtil;
import org.omnione.did.common.util.DidUtil;
import org.omnione.did.common.util.DidValidator;
import org.omnione.did.common.util.JsonUtil;
import org.omnione.did.crypto.keypair.KeyPairInterface;
import org.omnione.did.data.model.did.DidDocument;
import org.omnione.did.tas.v1.common.service.DidDocService;
import org.omnione.did.tas.v1.common.service.query.ApiQueryService;
import org.omnione.did.tas.v1.common.service.query.TasQueryService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.util.MlKemUtils;

/**
 * Implementation of the EcdhService interface for handling ECDH (Elliptic Curve Diffie-Hellman) key exchange.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!sample")
public class EcdhServiceImpl implements EcdhService {

    private final TransactionService transactionService;
    private final DidDocService didDocService;
    private final EcdhRepository ecdhRepository;
    private final FileWalletService fileWalletService;
    private final ApiQueryService apiQueryService;
    private final TasQueryService tasQueryService;

    /**
     * Handles the ECDH request process.
     *
     * @param requestECDHReqDto The DTO containing the ECDH request details
     * @return RequestECDHResDto The response DTO containing the ECDH response details
     * @throws OpenDidException if there's an error during the ECDH process
     */
    @Override
    public RequestECDHResDto requestECDH(RequestECDHReqDto requestECDHReqDto) {
        try {
            log.info("=== Starting requestECDH ===");
            // Retrieve Transaction information.
            log.debug("\t--> Retrieving Transaction information");
            Transaction transaction = transactionService.findTransactionByTxId(requestECDHReqDto.getTxId());
            SubTransaction lastSubTransaction = transactionService.findLastSubTransaction(transaction.getId());

            // Validate transaction's validity.
            log.debug("\t--> Validating transaction's validity");
            validateTransaction(transaction, lastSubTransaction);

            // Verify Signature.
            log.debug("\t--> Verifying signature");
            verifyReqEcdh(requestECDHReqDto.getReqEcdh(), transaction);

            // Generate session key, and save information, and generate response message.
            log.debug("\t--> Generating session key and response data");
            RequestECDHResDto requestECDHResDto = generateSessionKeyAndResponseData(requestECDHReqDto, transaction, lastSubTransaction);
            log.debug("*** Finished requestECDH ***");

            return requestECDHResDto;
        } catch (OpenDidException e) {
            log.error("OpenDidException occurred during requestECDH: {}", e.getErrorCode().getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Exception occurred during requestECDH: {}", e.getMessage(), e);
            throw new OpenDidException(ErrorCode.FAIL_TO_REQUEST_ECDH);
        }
    }

    /**
     * Validates the transaction and sub-transaction for the ECDH process.
     *
     * @param transaction The transaction to validate
     * @param subTransaction The sub-transaction to validate
     * @throws OpenDidException if the transaction is invalid or expired
     */
    private void validateTransaction(Transaction transaction, SubTransaction subTransaction) {
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new OpenDidException(ErrorCode.TRANSACTION_INVALID);
        }
        if (DateTimeUtil.isExpired(transaction.getExpiredAt())) {
            throw new OpenDidException(ErrorCode.TRANSACTION_EXPIRED);
        }

        Set<SubTransactionType> VALID_TYPES = EnumSet.of(
                SubTransactionType.PROPOSE_ENROLL_ENTITY,
                SubTransactionType.PROPOSE_REGISTER_USER,
                SubTransactionType.PROPOSE_ISSUE_VC,
                SubTransactionType.PROPOSE_UPDATE_DIDDOC,
                SubTransactionType.PROPOSE_RESTORE_DIDDOC,
                SubTransactionType.PROPOSE_REVOKE_VC
        );
        if (!VALID_TYPES.contains(subTransaction.getType())) {
            throw new OpenDidException(ErrorCode.TRANSACTION_INVALID);
        }
    }

    /**
     * Verifies the ECDH request data.
     *
     * @param ecdhReqData The ECDH request data to verify
     * @param transaction The associated transaction
     * @throws OpenDidException if the verification fails
     */
    private void verifyReqEcdh(EcdhReqData ecdhReqData, Transaction transaction) {

        // Extract and validate did and didKeyUrl
        String clientDid = ecdhReqData.getClient();
        if (!DidValidator.isValidDid(clientDid)){
            log.error("Invalid DID: {}", clientDid);
            throw new OpenDidException(ErrorCode.INVALID_SIGNATURE);
        }

        String verificationMethod = ecdhReqData.getProof().getVerificationMethod();
        if (!DidValidator.isValidDidKeyUrl(verificationMethod)) {
            log.error("Invalid DID key URL: {}", verificationMethod);
            throw new OpenDidException(ErrorCode.INVALID_SIGNATURE);
        }

        // Check the equivalence of did.
        String didOfKeyUrl = DidUtil.extractDid(verificationMethod);
        if (!clientDid.equals(didOfKeyUrl)) {
            log.error("DID mismatch: clientDid={}, didOfKeyUrl={}", clientDid, didOfKeyUrl);
            throw new OpenDidException(ErrorCode.INVALID_SIGNATURE);
        }

        // Check the purpose of the proof.
        if (ecdhReqData.getProof().getProofPurpose() != ProofPurpose.KEY_AGREEMENT) {
            log.error("Invalid proof purpose: {}", ecdhReqData.getProof().getProofPurpose());
            throw new OpenDidException(ErrorCode.INVALID_SIGNATURE);
        }

        // Extract the signature message.
        byte[] signatureMessage = extractSignatureMessage(ecdhReqData);

        // Find Wallet Provider DID Document.
        DidDocument clientDidDocument = didDocService.getDidDocument(verificationMethod);

        // Get the keyagree public key.
        String encodedKeyAgreePublicKey = BaseCoreDidUtil.getPublicKey(clientDidDocument, "keyagree");

        // Verify the signature.
        verifySignature(encodedKeyAgreePublicKey, ecdhReqData.getProof().getProofValue(), signatureMessage, ecdhReqData.getProof().getType());
    }

    /**
     * Extracts the signature message from the ECDH request data.
     *
     * @param data The ECDH request data
     * @return byte[] The extracted signature message
     * @throws OpenDidException if the extraction fails
     */
    private byte[] extractSignatureMessage(EcdhReqData data) {
        try {
            // Remove proofValue from Proof fields in the object.
            EcdhReqData signatureMessageObject = removeProofValue(data);

            // Serialize to JSON and remove whitespaces.
            String jsonString = JsonUtil.serializeAndSort(signatureMessageObject);

            // Hash with SHA-256
            return BaseDigestUtil.generateHash(jsonString);
        } catch(CommonSdkException e) {
            throw new OpenDidException(ErrorCode.SIGNATURE_VERIFICATION_FAILED);
        }
    }

    /**
     * Removes the proof value from the ECDH request data.
     *
     * @param data The original ECDH request data
     * @return EcdhReqData The ECDH request data with proof value removed
     */
    private EcdhReqData removeProofValue(EcdhReqData data) {
        EcdhReqData signatureMessageObject = EcdhReqData.builder()
                .client(data.getClient())
                .clientNonce(data.getClientNonce())
                .curve(data.getCurve())
                .algorithm(data.getAlgorithm())
                .publicKey(data.getPublicKey())
                .candidate(data.getCandidate())
                .proof(new Proof(
                        data.getProof().getType(),
                        data.getProof().getCreated(),
                        data.getProof().getVerificationMethod(),
                        data.getProof().getProofPurpose(),
                        null
                ))
                .build();

        return signatureMessageObject;
    }

    /**
     * Verifies the signature of the ECDH request.
     *
     * @param encodedPublicKey The encoded public key
     * @param signature The signature to verify
     * @param signatureMassage The original message that was signed
     * @param proofType The type of proof used for the signature
     * @throws OpenDidException if the signature verification fails
     */
    //@TODO: 공통함수로 빼야 함
    private void verifySignature(String encodedPublicKey, String signature, byte[] signatureMassage, ProofType proofType) {
        try {
            BaseCryptoUtil.verifySignature(encodedPublicKey, signature, signatureMassage, proofType.toEccCurveType());
        } catch (OpenDidException e) {
            throw e;
        } catch (Exception e) {
            log.error("\t--> Exception occurred during verifySignature: {}", e.getMessage(), e);
            throw new OpenDidException(ErrorCode.SIGNATURE_VERIFICATION_FAILED);
        }
    }

    /**
     * Determines the cipher type to use for the ECDH process.
     *
     * @param candidate The candidate cipher types
     * @return SymmetricCipherType The determined cipher type
     * @throws OpenDidException if no matching cipher type is found
     */
    private SymmetricCipherType determineCipherType(Candidate candidate) {
        SymmetricCipherType serverCipherType = getServerCipherType();
        if (candidate == null) {
            return serverCipherType;
        }

        return candidate.getCiphers().stream()
                .filter(cipher -> cipher == serverCipherType)
                .findFirst()
                .orElseThrow(() -> new OpenDidException(ErrorCode.NO_MATCHING_CIPHER_TYPE));
    }

    /**
     * Retrieves the server's cipher type from the configuration.
     *
     * @return SymmetricCipherType The server's cipher type
     * @throws OpenDidException if the server configuration is invalid
     */
    private SymmetricCipherType getServerCipherType() {
        try {
            return apiQueryService.findCipherType();
        } catch (IllegalArgumentException e) {
            throw new OpenDidException(ErrorCode.INVALID_SERVER_CONFIGURATION);
        }
    }

    /**
     * Determines the padding type to use for the ECDH process.
     *
     * @return SymmetricPaddingType The determined padding type
     * @throws OpenDidException if the server configuration is invalid
     */
    private SymmetricPaddingType determinePaddingType() {
        try {
            return apiQueryService.findPaddingType();
        } catch (IllegalArgumentException e){
            throw new OpenDidException(ErrorCode.INVALID_SERVER_CONFIGURATION);
        }
    }

    private void validateClientNonce(String encodedClientNonce) {
        byte[] clientNonce = BaseMultibaseUtil.decode(encodedClientNonce);
        if (clientNonce.length != 16) {
            throw new OpenDidException(ErrorCode.INVALID_CLIENT_NONCE);
        }
    }

    /**
     * Merges the client and server nonces.
     *
     * @param encodedClientNonce The encoded client nonce
     * @param serverNonce The server nonce
     * @return byte[] The merged nonce
     * @throws OpenDidException if the nonce generation fails
     */
    private byte[] mergeNonces(String encodedClientNonce, byte[] serverNonce) {
        try {
            byte[] clientNonce = BaseMultibaseUtil.decode(encodedClientNonce);
            byte[] combinedNonce = new byte[serverNonce.length + clientNonce.length];
            System.arraycopy(clientNonce, 0, combinedNonce, 0, clientNonce.length);
            System.arraycopy(serverNonce, 0, combinedNonce, clientNonce.length, serverNonce.length);

            return BaseDigestUtil.generateHash(combinedNonce);
        } catch (IllegalArgumentException e) {
            throw new OpenDidException(ErrorCode.NONCE_GENERATION_FAILED);
        }
    }

    /**
     * Generates the session key and response data for the ECDH/ML-KEM process.
     * ML-KEM-768이 요청된 경우 ML-KEM 경로로 분기하고, 그 외에는 기존 ECDH 경로를 사용한다.
     */
    private RequestECDHResDto generateSessionKeyAndResponseData(RequestECDHReqDto requestECDHReqDto, Transaction transaction, SubTransaction lastSubTransaction) {
        if (isMLKEM(requestECDHReqDto.getReqEcdh())) {
            return generateMLKEMSessionKeyAndResponseData(requestECDHReqDto, transaction, lastSubTransaction);
        }
        return generateECDHSessionKeyAndResponseData(requestECDHReqDto, transaction, lastSubTransaction);
    }

    /**
     * candidate.keyAgreements 또는 algorithm 필드를 보고 ML-KEM 요청인지 판별한다.
     * keyAgreements가 있으면 ML-KEM-768 포함 여부로, 없으면 algorithm 필드로 판단한다.
     */
    private boolean isMLKEM(EcdhReqData reqEcdh) {
        List<String> keyAgreements = reqEcdh.getCandidate() != null
                ? reqEcdh.getCandidate().getKeyAgreements()
                : null;
        if (keyAgreements != null && !keyAgreements.isEmpty()) {
            return keyAgreements.contains("ML-KEM-768");
        }
        return "ML-KEM-768".equals(reqEcdh.getAlgorithm());
    }

    /**
     * ML-KEM-768 세션키 생성 및 응답 데이터 반환.
     *
     * 흐름:
     *  1. 클라이언트 ML-KEM 공개키 복원
     *  2. encapsulate → sharedSecret + ciphertext
     *  3. mergedNonce = SHA-256(clientNonce || serverNonce)
     *  4. sessionKey = mergeSharedSecretAndNonce(sharedSecret, mergedNonce, cipherAlg)
     *  5. accEcdh.ciphertext + selectedAlgorithm 반환
     */
    private RequestECDHResDto generateMLKEMSessionKeyAndResponseData(RequestECDHReqDto requestECDHReqDto, Transaction transaction, SubTransaction lastSubTransaction) {
        try {
            EcdhReqData reqEcdh = requestECDHReqDto.getReqEcdh();

            // 1. 클라이언트 임시 ML-KEM 공개키 복원
            byte[] clientKemPubKeyBytes = BaseMultibaseUtil.decode(reqEcdh.getPublicKey());
            java.security.PublicKey clientKemPubKey = MlKemUtils.restorePublicKey(clientKemPubKeyBytes);

            // 2. Encapsulate: sharedSecret + ciphertext 생성
            MlKemUtils.EncapsulationResult encResult = MlKemUtils.encapsulate(clientKemPubKey);

            // 3. serverNonce 생성
            byte[] serverNonce = BaseCryptoUtil.generateNonce(16);
            String encodedServerNonce = BaseMultibaseUtil.encode(serverNonce);

            // 4. clientNonce 검증 및 mergedNonce 생성
            validateClientNonce(reqEcdh.getClientNonce());
            byte[] mergedNonce = mergeNonces(reqEcdh.getClientNonce(), serverNonce);
            String encodedMergedNonce = BaseMultibaseUtil.encode(mergedNonce);

            // 5. Cipher/Padding 협상
            SymmetricCipherType symmetricCipherType = determineCipherType(reqEcdh.getCandidate());
            SymmetricPaddingType symmetricPaddingType = determinePaddingType();

            // 6. 세션키 도출: mergeSharedSecretAndNonce 재사용 (PoC deriveSessionKey와 동일 로직이지만 cipher 길이 잘라냄 포함)
            byte[] sessionKey = BaseCryptoUtil.mergeSharedSecretAndNonce(encResult.sharedSecret(), mergedNonce, symmetricCipherType);
            String encodedSessionKey = BaseMultibaseUtil.encode(sessionKey);

            // 7. ECDH 정보 저장 (기존 테이블 재사용)
            insertEcdh(reqEcdh.getClient(), encodedSessionKey, encodedMergedNonce, symmetricCipherType, symmetricPaddingType, transaction.getId());

            // 8. AccEcdh 생성 (ciphertext 사용, publicKey 없음)
            Tas existedTas = tasQueryService.findTas();
            String tasDid = existedTas.getDid();
            DidDocument tasDidDocument = didDocService.getDidDocument(tasDid);

            AccEcdh unsignedAccEcdh = AccEcdh.builder()
                    .server(tasDid)
                    .serverNonce(encodedServerNonce)
                    .ciphertext(BaseMultibaseUtil.encode(encResult.ciphertext()))
                    .selectedAlgorithm("ML-KEM-768")
                    .cipher(symmetricCipherType)
                    .padding(symmetricPaddingType)
                    .proof(Proof.builder()
                            .type(ProofType.SECP_256R1_SIGNATURE_2018)
                            .created(DateTimeUtil.getCurrentUTCTimeString())
                            .verificationMethod(didDocService.getVerificationMethod(tasDidDocument, ProofPurpose.KEY_AGREEMENT))
                            .proofPurpose(ProofPurpose.KEY_AGREEMENT)
                            .proofValue(null)
                            .build())
                    .build();

            byte[] signatureMessage = extractSignatureMessage(unsignedAccEcdh);
            String proofValue = sign(signatureMessage, ProofPurpose.KEY_AGREEMENT);
            AccEcdh signedAccEcdh = addProofValue(unsignedAccEcdh, proofValue);

            transactionService.insertSubTransaction(SubTransaction.builder()
                    .transactionId(transaction.getId())
                    .step(lastSubTransaction.getStep() + 1)
                    .type(SubTransactionType.REQUEST_ECDH)
                    .status(SubTransactionStatus.COMPLETED)
                    .build());

            return RequestECDHResDto.builder()
                    .txId(requestECDHReqDto.getTxId())
                    .accEcdh(signedAccEcdh)
                    .build();

        } catch (CryptoException e) {
            log.error("ML-KEM session key generation failed: {}", e.getMessage(), e);
            throw new OpenDidException(ErrorCode.SESSION_KEY_GENERATION_FAILED);
        }
    }

    /**
     * 기존 ECDH 세션키 생성 및 응답 데이터 반환 (변경 없음).
     */
    private RequestECDHResDto generateECDHSessionKeyAndResponseData(RequestECDHReqDto requestECDHReqDto, Transaction transaction, SubTransaction lastSubTransaction) {

        // Retrieve TAS information.
        Tas existedTas = tasQueryService.findTas();

        // Get client public key.
        byte[] clientPublicKey = BaseMultibaseUtil.decode(requestECDHReqDto.getReqEcdh().getPublicKey());

        // Generate server key pair.
        KeyPairInterface keyPairInterface = BaseCryptoUtil.generateKeyPair(requestECDHReqDto.getReqEcdh().getCurve());
        byte[] serverPublicKey = ((ECPublicKey) keyPairInterface.getPublicKey()).getEncoded();
        byte[] serverPrivateKey = ((ECPrivateKey) keyPairInterface.getPrivateKey()).getEncoded();
        byte[] compressPublicKey = BaseCryptoUtil.compressPublicKey(serverPublicKey, requestECDHReqDto.getReqEcdh().getCurve());

        String encodedServerPublicKey = BaseMultibaseUtil.encode(compressPublicKey);

        // Generate serverNonce.
        byte[] serverNonce = BaseCryptoUtil.generateNonce(16);
        String encodedServerNonce = BaseMultibaseUtil.encode(serverNonce);

        // Merge clientNonce and serverNonce.
        validateClientNonce(requestECDHReqDto.getReqEcdh().getClientNonce());

        byte[] mergedNonce = mergeNonces(requestECDHReqDto.getReqEcdh().getClientNonce(), serverNonce);
        String encodedMergedNonce = BaseMultibaseUtil.encode(mergedNonce);

        // Choose Cipher algorithm and padding type.
        SymmetricCipherType symmetricCipherType = determineCipherType(requestECDHReqDto.getReqEcdh().getCandidate());
        SymmetricPaddingType symmetricPaddingType = determinePaddingType();

        // Generate session key.
        byte[] sessionKey = generateSessionKey(clientPublicKey, serverPrivateKey, mergedNonce, symmetricCipherType, requestECDHReqDto.getReqEcdh().getCurve());
        String encodedSessionKey = BaseMultibaseUtil.encode(sessionKey);

        // Insert ECDH information
        insertEcdh(requestECDHReqDto.getReqEcdh().getClient(), encodedSessionKey, encodedMergedNonce, symmetricCipherType, symmetricPaddingType, transaction.getId());

        // Retrieve TAS did document.
        String tasDid = existedTas.getDid();
        DidDocument tasDidDocument = didDocService.getDidDocument(tasDid);

        // Generate AccEcdh
        AccEcdh unsignedAccEcdh = AccEcdh.builder()
                .server(tasDid)
                .serverNonce(encodedServerNonce)
                .publicKey(encodedServerPublicKey)
                .cipher(symmetricCipherType)
                .padding(symmetricPaddingType)
                .proof(Proof.builder()
                        .type(ProofType.SECP_256R1_SIGNATURE_2018)
                        .created(DateTimeUtil.getCurrentUTCTimeString())
                        .verificationMethod(didDocService.getVerificationMethod(tasDidDocument, ProofPurpose.KEY_AGREEMENT))
                        .proofPurpose(ProofPurpose.KEY_AGREEMENT)
                        .proofValue(null)
                        .build())
                .build();

        // Extract the signature message.
        byte[] signatureMessage = extractSignatureMessage(unsignedAccEcdh);

        // Sign AccEcdh.
        String proofValue = sign(signatureMessage, ProofPurpose.KEY_AGREEMENT);

        // Re-generate AccEcdh with proofValue.
        AccEcdh signedAccEcdh = addProofValue(unsignedAccEcdh, proofValue);

        // Insert sub-transaction information.
        transactionService.insertSubTransaction(SubTransaction.builder()
                .transactionId(transaction.getId())
                .step(lastSubTransaction.getStep() + 1)
                .type(SubTransactionType.REQUEST_ECDH)
                .status(SubTransactionStatus.COMPLETED)
                .build()
        );

        return RequestECDHResDto.builder()
                .txId(requestECDHReqDto.getTxId())
                .accEcdh(signedAccEcdh)
                .build();
    }

    /**
     * Generates the session key using ECDH.
     *
     * @param compressedClientPublicKey The compressed client public key
     * @param serverPrivateKey The server private key
     * @param mergedNonce The merged nonce
     * @param symmetricCipherType The symmetric cipher type
     * @param eccCurveType The elliptic curve type
     * @return byte[] The generated session key
     */
    private byte[] generateSessionKey(byte[] compressedClientPublicKey, byte[] serverPrivateKey, byte[] mergedNonce, SymmetricCipherType symmetricCipherType, EccCurveType eccCurveType) {
        byte[] sharedSecret = BaseCryptoUtil.generateSharedSecret(compressedClientPublicKey, serverPrivateKey, eccCurveType);
        return BaseCryptoUtil.mergeSharedSecretAndNonce(sharedSecret, mergedNonce, symmetricCipherType);
    }

    /**
     * Inserts the ECDH information into the repository.
     *
     * @param client The client DID
     * @param encodedSessionKey The encoded session key
     * @param encodedMergedNonce The encoded merged nonce
     * @param symmetricCipherType The symmetric cipher type
     * @param symmetricPaddingType The symmetric padding type
     * @param transactionId The transaction ID
     */
    //@TODO: Nonce는 DB에 저장할 필요가 없음. 삭제할 것.
    private void insertEcdh(String client, String encodedSessionKey, String encodedMergedNonce, SymmetricCipherType symmetricCipherType, SymmetricPaddingType symmetricPaddingType, Long transactionId) {
        Ecdh ecdh = Ecdh.builder()
                .clientDid(client)
                .nonce(encodedMergedNonce)
                .sessionKey(encodedSessionKey)
                .cipher(symmetricCipherType.toString())
                .padding(symmetricPaddingType.toString())
                .transactionId(transactionId)
                .build();

        ecdhRepository.save(ecdh);
    }

    /**
     * Adds the proof value to the AccEcdh object.
     *
     * @param accEcdh The original AccEcdh object
     * @param proofValue The proof value to add
     * @return AccEcdh The AccEcdh object with the proof value added
     */
    private AccEcdh addProofValue(AccEcdh accEcdh, String proofValue) {
        return AccEcdh.builder()
                .server(accEcdh.getServer())
                .serverNonce(accEcdh.getServerNonce())
                .publicKey(accEcdh.getPublicKey())
                .ciphertext(accEcdh.getCiphertext())           // PQC: ML-KEM 응답 시 사용
                .selectedAlgorithm(accEcdh.getSelectedAlgorithm()) // PQC: 선택된 알고리즘
                .cipher(accEcdh.getCipher())
                .padding(accEcdh.getPadding())
                .proof(Proof.builder()
                        .type(accEcdh.getProof().getType())
                        .created(accEcdh.getProof().getCreated())
                        .verificationMethod(accEcdh.getProof().getVerificationMethod())
                        .proofPurpose(accEcdh.getProof().getProofPurpose())
                        .proofValue(proofValue)
                        .build())
                .build();
    }

    /**
     * Extracts the signature message from the AccEcdh object.
     *
     * @param accEcdh The AccEcdh object
     * @return byte[] The extracted signature message
     * @throws OpenDidException if the extraction fails
     */
    private byte[] extractSignatureMessage(AccEcdh accEcdh) {
        try {
            // Serialize to JSON and remove whitespaces.
            String jsonString = JsonUtil.serializeAndSort(accEcdh);

            // Hash with SHA-256
            return BaseDigestUtil.generateHash(jsonString);
        } catch (CommonSdkException e) {
            log.error("Failed to Json Processing: {}", e.getMessage(), e);
            throw new OpenDidException(ErrorCode.JSON_PROCESSING_ERROR);
        } catch (Exception e) {
            log.error("Failed to extract signature message: {}", e.getMessage(), e);
            throw new OpenDidException(ErrorCode.RESPONSE_SIGNATURE_FAILED);
        }
    }

    /**
     * Signs the data using the specified proof purpose.
     *
     * @param data The data to sign
     * @param proofPurpose The proof purpose
     * @return String The generated signature
     */
    private String sign(byte[] data, ProofPurpose proofPurpose) {
        byte[] signatureBytes = fileWalletService.generateCompactSignature(proofPurpose.toKeyId(), data);
        return BaseMultibaseUtil.encode(signatureBytes);
    }

    /**
     * 성능 측정용 키교환 단독 호출.
     *
     * 흐름:
     *  1. TAS keyagree 공개키 사전 조회 (타이밍 외부)
     *  2. ML-KEM-768 이면 encapsulate, 그 외 ECDH 이면 서버 임시 키쌍 생성 후 shared secret 계산
     *  3. AccEcdh 서명과 동일하게 32-byte zeros 해시를 KEY_AGREEMENT 키로 서명
     *  4. 트랜잭션/DB 저장을 건너뛰고 키교환+서명 crypto 시간만 측정
     *  5. 클라이언트 서명 검증에 필요한 데이터(ciphertext/serverPublicKey, proofValue, tasKeyAgreePublicKey) 반환
     *
     * algorithm 필드가 "ML-KEM-768" 이면 ML-KEM 경로, 아니면 curve 값으로 ECDH 경로.
     */
    @Override
    public TestRequestEcdhOnlyResDto requestECDHOnly(TestRequestEcdhOnlyReqDto req) {
        boolean isMlKem = "ML-KEM-768".equalsIgnoreCase(req.getAlgorithm());
        String selectedAlgorithm = isMlKem ? "ML-KEM-768"
                : (req.getCurve() != null ? req.getCurve() : "Secp256r1");

        try {
            // TAS keyagree 공개키 사전 조회 (타이밍 외부 — DB/네트워크 I/O 제외)
            Tas existedTas = tasQueryService.findTas();
            DidDocument tasDidDocument = didDocService.getDidDocument(existedTas.getDid());
            String tasKeyAgreePublicKey = BaseCoreDidUtil.getPublicKey(tasDidDocument, "keyagree");

            byte[] clientPublicKey = BaseMultibaseUtil.decode(req.getPublicKey());
            // AccEcdh 서명 메시지와 동일한 크기(SHA-256 해시 = 32바이트)의 더미 해시
            byte[] dummySignatureMessage = new byte[32];

            String ciphertext = null;
            String serverPublicKey = null;

            long startNs = System.nanoTime();

            if (isMlKem) {
                java.security.PublicKey clientKemPubKey = MlKemUtils.restorePublicKey(clientPublicKey);
                MlKemUtils.EncapsulationResult encResult = MlKemUtils.encapsulate(clientKemPubKey);
                ciphertext = BaseMultibaseUtil.encode(encResult.ciphertext());
            } else {
                EccCurveType curveType = EccCurveType.SECP_256_R1;
                if ("Secp256k1".equalsIgnoreCase(req.getCurve())) {
                    curveType = EccCurveType.SECP_256_K1;
                }
                KeyPairInterface keyPairInterface = BaseCryptoUtil.generateKeyPair(curveType);
                byte[] serverPrivKey = ((ECPrivateKey) keyPairInterface.getPrivateKey()).getEncoded();
                byte[] serverPubKey  = ((ECPublicKey)  keyPairInterface.getPublicKey()).getEncoded();
                byte[] compressed    = BaseCryptoUtil.compressPublicKey(serverPubKey, curveType);
                BaseCryptoUtil.generateSharedSecret(clientPublicKey, serverPrivKey, curveType);
                serverPublicKey = BaseMultibaseUtil.encode(compressed);
            }

            // TAS keyagree 키 알고리즘 판별: SubjectPublicKeyInfo(0x30) → ML-DSA-44, 압축 EC(0x02/0x03) → Secp256r1
            // TAS keyagree 키 알고리즘 판별: SubjectPublicKeyInfo(0x30) → ML-DSA-44, 압축 EC(0x02/0x03) → Secp256r1
            byte[] tasKeyBytes = BaseMultibaseUtil.decode(tasKeyAgreePublicKey);
            String keyAgreeAlgorithm = (tasKeyBytes[0] == 0x30)
                    ? "MlDsa44Signature2024"
                    : "Secp256r1Signature2018";

            // ML-DSA-44: 원본 데이터 직접 서명, ECDSA: SHA-256 해싱 후 서명
            byte[] signatureBytes = fileWalletService.generateCompactSignature(
                    ProofPurpose.KEY_AGREEMENT.toKeyId(), dummySignatureMessage, keyAgreeAlgorithm);
            String proofValue = BaseMultibaseUtil.encode(signatureBytes);

            long elapsedNs = System.nanoTime() - startNs;
            double elapsedMs = elapsedNs / 1_000_000.0;

            log.debug("requestECDHOnly algorithm={} elapsedMs={}", selectedAlgorithm, elapsedMs);

            return TestRequestEcdhOnlyResDto.builder()
                    .serverProcessingMs(elapsedMs)
                    .result("ok")
                    .algorithm(selectedAlgorithm)
                    .ciphertext(ciphertext)
                    .serverPublicKey(serverPublicKey)
                    .proofValue(proofValue)
                    .tasKeyAgreePublicKey(tasKeyAgreePublicKey)
                    .build();

        } catch (CryptoException e) {
            log.error("requestECDHOnly ML-KEM failure: {}", e.getMessage(), e);
            throw new OpenDidException(ErrorCode.SESSION_KEY_GENERATION_FAILED);
        } catch (OpenDidException e) {
            throw e;
        } catch (Exception e) {
            log.error("requestECDHOnly failure: {}", e.getMessage(), e);
            throw new OpenDidException(ErrorCode.FAIL_TO_REQUEST_ECDH);
        }
    }
}
