package org.omnione.did.poc.pqc.crypto.keyagree;

import org.omnione.did.data.model.DataObject;
import org.omnione.did.data.model.did.Proof;
import org.omnione.did.data.model.util.json.GsonWrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * ML-KEM 키 교환 요청 메시지.
 *
 * 기존 ReqE2e(ECDH 기반)와 대응되는 구조:
 *   ReqE2e:   { nonce, curve, publicKey, cipher, padding, proof }
 *   ReqMLKEM: { nonce, algorithm, publicKey, cipher, padding, proof }
 *
 * Alice → Bob: "내 ML-KEM 공개키와 nonce를 보내니, 이걸로 캡슐화해줘"
 */
public class ReqMLKEM extends DataObject {

    /** Nonce for symmetric key derivation (16 bytes, multibase encoded) */
    @SerializedName("nonce")
    @Expose
    private String nonce;

    /** ML-KEM 알고리즘 (e.g. "ML-KEM-768") - 기존 curve 필드 대응 */
    @SerializedName("algorithm")
    @Expose
    private String algorithm;

    /** 요청자의 ML-KEM 공개키 (multibase encoded) */
    @SerializedName("publicKey")
    @Expose
    private String publicKey;

    /** 대칭 암호 알고리즘 (e.g. "AES-256-CBC") */
    @SerializedName("cipher")
    @Expose
    private String cipher;

    /** 패딩 방식 (e.g. "PKCS5") */
    @SerializedName("padding")
    @Expose
    private String padding;

    /** ML-DSA 서명으로 요청의 무결성·인증 보장 */
    @SerializedName("proof")
    @Expose
    private Proof proof;

    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getCipher() { return cipher; }
    public void setCipher(String cipher) { this.cipher = cipher; }

    public String getPadding() { return padding; }
    public void setPadding(String padding) { this.padding = padding; }

    public Proof getProof() { return proof; }
    public void setProof(Proof proof) { this.proof = proof; }

    @Override
    public void fromJson(String val) {
        GsonWrapper gson = new GsonWrapper();
        ReqMLKEM data = gson.fromJson(val, ReqMLKEM.class);
        nonce = data.nonce;
        algorithm = data.algorithm;
        publicKey = data.publicKey;
        cipher = data.cipher;
        padding = data.padding;
        proof = data.proof;
    }
}
