package org.omnione.did.poc.pqc.crypto.keyagree;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.function.Function;

import org.omnione.did.data.model.did.DidDocument;
import org.omnione.did.data.model.did.Proof;
import org.omnione.did.poc.pqc.crypto.ECDHKeyManager;
import org.omnione.did.poc.pqc.util.DidDocumentUtil;

/**
 * ECDH 기반 키 교환 프로토콜.
 *
 * Wallet SDK 기반으로 리팩토링: 서명 시 Function<byte[], byte[]> signer를 받아
 * 개인키를 직접 다루지 않음.
 * Wallet SDK의 compact(v||R||S) 서명 포맷을 지원.
 */
public class ECDHProtocol {

    private static final String CURVE = "Secp256r1";
    private static final String PROOF_TYPE_SECP256R1 = "Secp256r1Signature2018";
    private static final String KEY_ID = "keyagree";

    private final ECDHKeyManager ecdhManager = new ECDHKeyManager();

    // Step 1: Alice가 reqECDH 생성
    public ReqECDHResult createReqECDH(DidDocument aliceDidDoc,
                                        Function<byte[], byte[]> signer) throws Exception {
        KeyPair ephemeralKeyPair = ecdhManager.generateKeyPair();

        byte[] clientNonce = new byte[16];
        new SecureRandom().nextBytes(clientNonce);

        ReqECDH req = new ReqECDH();
        req.setNonce(multibaseEncode(clientNonce));
        req.setCurve(CURVE);
        req.setPublicKey(multibaseEncode(ephemeralKeyPair.getPublic().getEncoded()));
        req.setCipher("AES-256-CBC");
        req.setPadding("PKCS5");

        String proofTarget = req.toJson();
        byte[] signature = signer.apply(proofTarget.getBytes());

        Proof proof = createProof(aliceDidDoc.getId(), KEY_ID, "keyAgreement", signature);
        req.setProof(proof);

        return new ReqECDHResult(req, ephemeralKeyPair);
    }

    // Step 2: Bob이 reqECDH 검증 → resECDH 생성
    public ECDHExchangeResult processReqECDH(ReqECDH reqECDH,
                                              DidDocument aliceDidDoc,
                                              DidDocument bobDidDoc,
                                              Function<byte[], byte[]> signer,
                                              byte[] serverNonce) throws Exception {
        // 1) Alice 서명 검증 (compact 서명 → DER 변환 후 검증)
        Proof reqProof = reqECDH.getProof();
        reqECDH.setProof(null);
        byte[] reqBody = reqECDH.toJson().getBytes();
        reqECDH.setProof(reqProof);

        PublicKey aliceVerifyKey = DidDocumentUtil.getPublicKey(aliceDidDoc, KEY_ID);
        byte[] reqSignature = multibaseDecode(reqProof.getProofValue());
        boolean valid = verifyEcCompactSignature(aliceVerifyKey, reqBody, reqSignature);
        if (!valid) {
            throw new SecurityException("reqECDH 서명 검증 실패");
        }

        // 2) Alice의 임시 EC 공개키 복원
        byte[] aliceEphPubKeyBytes = multibaseDecode(reqECDH.getPublicKey());
        KeyFactory kf = KeyFactory.getInstance("EC", "BC");
        PublicKey aliceEphPubKey = kf.generatePublic(new X509EncodedKeySpec(aliceEphPubKeyBytes));

        // 3) Bob의 임시 ECDH 키페어 생성
        KeyPair bobEphKeyPair = ecdhManager.generateKeyPair();

        // 4) ECDH → sharedSecret
        byte[] sharedSecret = ecdhManager.deriveSharedSecret(bobEphKeyPair.getPrivate(), aliceEphPubKey);

        // 5) 세션키 도출
        byte[] clientNonce = multibaseDecode(reqECDH.getNonce());
        byte[] mergedNonce = mergeNonce(clientNonce, serverNonce);
        byte[] sessionKey = deriveSessionKey(sharedSecret, mergedNonce);

        // 6) resECDH 생성
        ResECDH res = new ResECDH();
        res.setPublicKey(multibaseEncode(bobEphKeyPair.getPublic().getEncoded()));

        String resProofTarget = res.toJson();
        byte[] resSignature = signer.apply(resProofTarget.getBytes());
        Proof resProof = createProof(bobDidDoc.getId(), KEY_ID, "keyAgreement", resSignature);
        res.setProof(resProof);

        return new ECDHExchangeResult(res, sessionKey);
    }

    // Step 3: Alice가 resECDH 검증 → sessionKey 도출
    public byte[] processResECDH(ResECDH resECDH,
                                  DidDocument bobDidDoc,
                                  PrivateKey aliceEphemeralPrivKey,
                                  byte[] clientNonce,
                                  byte[] serverNonce) throws Exception {
        // 1) Bob 서명 검증 (compact 서명 → DER 변환 후 검증)
        Proof resProof = resECDH.getProof();
        resECDH.setProof(null);
        byte[] resBody = resECDH.toJson().getBytes();
        resECDH.setProof(resProof);

        PublicKey bobVerifyKey = DidDocumentUtil.getPublicKey(bobDidDoc, KEY_ID);
        byte[] resSignature = multibaseDecode(resProof.getProofValue());
        boolean valid = verifyEcCompactSignature(bobVerifyKey, resBody, resSignature);
        if (!valid) {
            throw new SecurityException("resECDH 서명 검증 실패");
        }

        // 2) Bob의 임시 공개키 복원
        byte[] bobEphPubKeyBytes = multibaseDecode(resECDH.getPublicKey());
        KeyFactory kf = KeyFactory.getInstance("EC", "BC");
        PublicKey bobEphPubKey = kf.generatePublic(new X509EncodedKeySpec(bobEphPubKeyBytes));

        // 3) ECDH → sharedSecret
        byte[] sharedSecret = ecdhManager.deriveSharedSecret(aliceEphemeralPrivKey, bobEphPubKey);

        // 4) 세션키 도출
        byte[] mergedNonce = mergeNonce(clientNonce, serverNonce);
        return deriveSessionKey(sharedSecret, mergedNonce);
    }

    // ──────────── EC Compact Signature Verify ────────────

    /**
     * Wallet SDK의 compact(v||R||S) 서명을 검증.
     * wallet sign()은 SHA-256 해시 후 compact signature를 생성하므로,
     * 검증 시에도 SHA-256 해시 후 NONEwithECDSA + DER 변환으로 검증.
     */
    private boolean verifyEcCompactSignature(PublicKey publicKey, byte[] data, byte[] compactSignature) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
        byte[] derSignature = compactToDer(compactSignature);
        Signature verifier = Signature.getInstance("NONEwithECDSA", "BC");
        verifier.initVerify(publicKey);
        verifier.update(hash);
        return verifier.verify(derSignature);
    }

    // compact(v||R||S 또는 R||S) → DER 변환
    private static byte[] compactToDer(byte[] compact) {
        int offset = (compact.length % 2 == 1) ? 1 : 0;
        int halfLen = (compact.length - offset) / 2;
        byte[] r = trimLeadingZeros(compact, offset, halfLen);
        byte[] s = trimLeadingZeros(compact, offset + halfLen, halfLen);
        boolean rPad = (r[0] & 0x80) != 0;
        boolean sPad = (s[0] & 0x80) != 0;
        int rLen = r.length + (rPad ? 1 : 0);
        int sLen = s.length + (sPad ? 1 : 0);
        byte[] der = new byte[2 + 2 + rLen + 2 + sLen];
        int idx = 0;
        der[idx++] = 0x30;
        der[idx++] = (byte)(2 + rLen + 2 + sLen);
        der[idx++] = 0x02;
        der[idx++] = (byte)rLen;
        if (rPad) der[idx++] = 0x00;
        System.arraycopy(r, 0, der, idx, r.length); idx += r.length;
        der[idx++] = 0x02;
        der[idx++] = (byte)sLen;
        if (sPad) der[idx++] = 0x00;
        System.arraycopy(s, 0, der, idx, s.length);
        return der;
    }

    private static byte[] trimLeadingZeros(byte[] data, int offset, int length) {
        int start = offset;
        while (start < offset + length - 1 && data[start] == 0) start++;
        byte[] result = new byte[offset + length - start];
        System.arraycopy(data, start, result, 0, result.length);
        return result;
    }

    // ──────────── Key Derivation ────────────

    static byte[] mergeNonce(byte[] clientNonce, byte[] serverNonce) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(clientNonce);
        digest.update(serverNonce);
        return digest.digest();
    }

    static byte[] deriveSessionKey(byte[] sharedSecret, byte[] mergedNonce) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(sharedSecret);
        digest.update(mergedNonce);
        return digest.digest();
    }

    private Proof createProof(String did, String keyId, String purpose, byte[] signature) {
        Proof proof = new Proof();
        proof.setType(PROOF_TYPE_SECP256R1);
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
            throw new IllegalArgumentException("Unsupported multibase prefix");
        }
        return Base64.getDecoder().decode(multibase.substring(1));
    }

    public record ReqECDHResult(ReqECDH reqECDH, KeyPair ephemeralKeyPair) {}
    public record ECDHExchangeResult(ResECDH resECDH, byte[] sessionKey) {}
}
