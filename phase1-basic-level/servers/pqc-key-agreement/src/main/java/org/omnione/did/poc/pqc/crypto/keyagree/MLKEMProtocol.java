package org.omnione.did.poc.pqc.crypto.keyagree;

import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.function.Function;

import org.omnione.did.data.model.did.DidDocument;
import org.omnione.did.data.model.did.Proof;
import org.omnione.did.poc.pqc.crypto.MLDSAKeyManager;
import org.omnione.did.poc.pqc.crypto.MLKEMKeyManager;
import org.omnione.did.poc.pqc.util.DidDocumentUtil;

/**
 * ML-KEM 기반 키 교환 프로토콜.
 *
 * Wallet SDK 기반으로 리팩토링: 서명 시 Function<byte[], byte[]> signer를 받아
 * 개인키를 직접 다루지 않음.
 */
public class MLKEMProtocol {

    private static final String ML_KEM_ALGORITHM = "ML-KEM-768";
    private static final String PROOF_TYPE_MLDSA44 = "MlDsa44Signature2024";
    private static final String KEY_ID = "keyagree";

    private final MLKEMKeyManager kemManager = new MLKEMKeyManager();
    private final MLDSAKeyManager dsaManager = new MLDSAKeyManager();

    // ──────────────────────────────────────────
    //  Step 1: Alice가 reqMLKEM 생성
    // ──────────────────────────────────────────

    /**
     * Alice가 임시(ephemeral) ML-KEM 키페어를 생성하고, 공개키와 clientNonce를 담은
     * 요청 메시지를 생성한다. Wallet SDK signer로 서명한다.
     *
     * @param aliceDidDoc Alice의 DID Document
     * @param signer      Wallet SDK 기반 서명 함수
     * @return ReqMLKEMResult (reqMLKEM + 임시 ML-KEM 개인키)
     */
    public ReqMLKEMResult createReqMLKEM(DidDocument aliceDidDoc,
                                          Function<byte[], byte[]> signer) throws Exception {
        // 임시(ephemeral) ML-KEM 키페어 생성
        KeyPair ephemeralKemKeyPair = kemManager.generateKeyPair();

        // clientNonce 생성 (16 bytes)
        byte[] clientNonce = new byte[16];
        new SecureRandom().nextBytes(clientNonce);

        ReqMLKEM req = new ReqMLKEM();
        req.setNonce(multibaseEncode(clientNonce));
        req.setAlgorithm(ML_KEM_ALGORITHM);
        req.setPublicKey(multibaseEncode(ephemeralKemKeyPair.getPublic().getEncoded()));
        req.setCipher("AES-256-CBC");
        req.setPadding("PKCS5");

        // proof 생성: Wallet SDK signer로 서명
        String proofTarget = req.toJson();
        byte[] signature = signer.apply(proofTarget.getBytes());

        Proof proof = createProof(
                aliceDidDoc.getId(),
                KEY_ID,
                ProofPurposeValue.KEY_AGREEMENT,
                signature
        );
        req.setProof(proof);

        return new ReqMLKEMResult(req, ephemeralKemKeyPair.getPrivate());
    }

    // ──────────────────────────────────────────
    //  Step 2: Bob이 reqMLKEM 검증 → resMLKEM 생성
    // ──────────────────────────────────────────

    /**
     * Bob이 Alice의 reqMLKEM을 검증하고, Alice의 임시 공개키로 encapsulate한 후
     * ciphertext를 담은 응답을 생성한다.
     *
     * @param reqMLKEM    Alice가 보낸 요청 메시지
     * @param aliceDidDoc Alice의 DID Document (서명 검증용)
     * @param bobDidDoc   Bob의 DID Document
     * @param signer      Bob의 Wallet SDK 기반 서명 함수
     * @param serverNonce Profile에서 전달된 serverNonce
     * @return MLKEMExchangeResult (resMLKEM + sessionKey)
     */
    public MLKEMExchangeResult processReqMLKEM(ReqMLKEM reqMLKEM,
                                               DidDocument aliceDidDoc,
                                               DidDocument bobDidDoc,
                                               Function<byte[], byte[]> signer,
                                               byte[] serverNonce) throws Exception {
        // 1) Alice의 서명 검증 (DID Document의 keyAgreement 공개키 사용)
        Proof reqProof = reqMLKEM.getProof();
        reqMLKEM.setProof(null);
        byte[] reqBody = reqMLKEM.toJson().getBytes();
        reqMLKEM.setProof(reqProof);

        PublicKey aliceVerifyKey = DidDocumentUtil.getPublicKey(aliceDidDoc, KEY_ID);
        byte[] reqSignature = multibaseDecode(reqProof.getProofValue());
        boolean valid = dsaManager.verify(aliceVerifyKey, reqBody, reqSignature);
        if (!valid) {
            throw new SecurityException("reqMLKEM 서명 검증 실패: Alice의 서명이 유효하지 않습니다");
        }

        // 2) Alice의 임시 ML-KEM 공개키 복원
        byte[] aliceKemPubKeyBytes = multibaseDecode(reqMLKEM.getPublicKey());
        java.security.KeyFactory kf = java.security.KeyFactory.getInstance(ML_KEM_ALGORITHM, "BC");
        PublicKey aliceKemPubKey = kf.generatePublic(
                new java.security.spec.X509EncodedKeySpec(aliceKemPubKeyBytes));

        // 3) Alice의 임시 공개키로 encapsulate → (kemSharedSecret, ciphertext)
        MLKEMKeyManager.EncapsulationResult encResult = kemManager.encapsulate(aliceKemPubKey);

        // 4) 세션키 도출: mergedNonce + kemSharedSecret → sessionKey
        byte[] clientNonce = multibaseDecode(reqMLKEM.getNonce());
        byte[] mergedNonce = mergeNonce(clientNonce, serverNonce);
        byte[] sessionKey = deriveSessionKey(encResult.sharedSecret(), mergedNonce);

        // 5) resMLKEM 생성
        ResMLKEM res = new ResMLKEM();
        res.setCiphertext(multibaseEncode(encResult.ciphertext()));

        // 6) resMLKEM에 Wallet SDK signer로 서명
        String resProofTarget = res.toJson();
        byte[] resSignature = signer.apply(resProofTarget.getBytes());

        Proof resProof = createProof(
                bobDidDoc.getId(),
                KEY_ID,
                ProofPurposeValue.KEY_AGREEMENT,
                resSignature
        );
        res.setProof(resProof);

        return new MLKEMExchangeResult(res, sessionKey);
    }

    // ──────────────────────────────────────────
    //  Step 3: Alice가 resMLKEM 검증 → sessionKey 도출
    // ──────────────────────────────────────────

    /**
     * Alice가 Bob의 resMLKEM을 검증하고, 임시 개인키로 decapsulate 후
     * nonce를 결합하여 sessionKey를 도출한다.
     *
     * @param resMLKEM              Bob이 보낸 응답 메시지
     * @param bobDidDoc             Bob의 DID Document (서명 검증용)
     * @param aliceEphemeralPrivKey Alice의 임시 ML-KEM 개인키
     * @param clientNonce           Alice가 reqMLKEM에 넣었던 clientNonce
     * @param serverNonce           Profile에서 전달된 serverNonce
     * @return 도출된 sessionKey
     */
    public byte[] processResMLKEM(ResMLKEM resMLKEM,
                                  DidDocument bobDidDoc,
                                  PrivateKey aliceEphemeralPrivKey,
                                  byte[] clientNonce,
                                  byte[] serverNonce) throws Exception {
        // 1) Bob의 서명 검증 (DID Document의 keyAgreement 공개키 사용)
        Proof resProof = resMLKEM.getProof();
        resMLKEM.setProof(null);
        byte[] resBody = resMLKEM.toJson().getBytes();
        resMLKEM.setProof(resProof);

        PublicKey bobVerifyKey = DidDocumentUtil.getPublicKey(bobDidDoc, KEY_ID);
        byte[] resSignature = multibaseDecode(resProof.getProofValue());
        boolean valid = dsaManager.verify(bobVerifyKey, resBody, resSignature);
        if (!valid) {
            throw new SecurityException("resMLKEM 서명 검증 실패: Bob의 서명이 유효하지 않습니다");
        }

        // 2) ciphertext를 임시 개인키로 decapsulate하여 kemSharedSecret 도출
        byte[] ciphertext = multibaseDecode(resMLKEM.getCiphertext());
        byte[] kemSharedSecret = kemManager.decapsulate(aliceEphemeralPrivKey, ciphertext);

        // 3) 세션키 도출: mergedNonce + kemSharedSecret → sessionKey
        byte[] mergedNonce = mergeNonce(clientNonce, serverNonce);
        return deriveSessionKey(kemSharedSecret, mergedNonce);
    }

    // ──────────── Key Derivation ────────────

    static byte[] mergeNonce(byte[] clientNonce, byte[] serverNonce) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(clientNonce);
        digest.update(serverNonce);
        return digest.digest();
    }

    static byte[] deriveSessionKey(byte[] kemSharedSecret, byte[] mergedNonce) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(kemSharedSecret);
        digest.update(mergedNonce);
        return digest.digest();
    }

    // ──────────── Helper ────────────

    private Proof createProof(String did, String keyId, String purpose, byte[] signature) {
        Proof proof = new Proof();
        proof.setType(PROOF_TYPE_MLDSA44);
        proof.setCreated(Instant.now().toString());
        proof.setVerificationMethod(did + "#" + keyId);
        proof.setProofPurpose(purpose);
        proof.setProofValue(multibaseEncode(signature));
        return proof;
    }

    static String multibaseEncode(byte[] data) {
        return "m" + Base64.getEncoder().encodeToString(data);
    }

    static byte[] multibaseDecode(String multibase) {
        if (!multibase.startsWith("m")) {
            throw new IllegalArgumentException("Unsupported multibase prefix: " + multibase.charAt(0));
        }
        return Base64.getDecoder().decode(multibase.substring(1));
    }

    /** reqMLKEM 생성 결과 (임시 ML-KEM 개인키 포함) */
    public record ReqMLKEMResult(ReqMLKEM reqMLKEM, PrivateKey ephemeralPrivateKey) {}

    /** 프로토콜 교환 결과 */
    public record MLKEMExchangeResult(ResMLKEM resMLKEM, byte[] sessionKey) {}

    /** ProofPurpose 값 상수 */
    private static final class ProofPurposeValue {
        static final String KEY_AGREEMENT = "keyAgreement";
    }
}
