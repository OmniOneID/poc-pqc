package org.omnione.did.poc.pqc.crypto.keyagree;

import org.omnione.did.data.model.DataObject;
import org.omnione.did.data.model.did.Proof;
import org.omnione.did.data.model.util.json.GsonWrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * ECDH 키 교환 요청 메시지.
 * ReqE2e와 동일한 구조: { nonce, curve, publicKey, cipher, padding, proof }
 */
public class ReqECDH extends DataObject {

    @SerializedName("nonce")
    @Expose
    private String nonce;

    @SerializedName("curve")
    @Expose
    private String curve;

    @SerializedName("publicKey")
    @Expose
    private String publicKey;

    @SerializedName("cipher")
    @Expose
    private String cipher;

    @SerializedName("padding")
    @Expose
    private String padding;

    @SerializedName("proof")
    @Expose
    private Proof proof;

    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }

    public String getCurve() { return curve; }
    public void setCurve(String curve) { this.curve = curve; }

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
        ReqECDH data = gson.fromJson(val, ReqECDH.class);
        nonce = data.nonce;
        curve = data.curve;
        publicKey = data.publicKey;
        cipher = data.cipher;
        padding = data.padding;
        proof = data.proof;
    }
}
