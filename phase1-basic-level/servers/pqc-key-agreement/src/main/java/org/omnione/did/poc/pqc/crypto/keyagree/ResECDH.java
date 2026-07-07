package org.omnione.did.poc.pqc.crypto.keyagree;

import org.omnione.did.data.model.DataObject;
import org.omnione.did.data.model.did.Proof;
import org.omnione.did.data.model.util.json.GsonWrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * ECDH 키 교환 응답 메시지.
 * Bob의 임시 공개키를 포함: { publicKey, proof }
 */
public class ResECDH extends DataObject {

    @SerializedName("publicKey")
    @Expose
    private String publicKey;

    @SerializedName("proof")
    @Expose
    private Proof proof;

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public Proof getProof() { return proof; }
    public void setProof(Proof proof) { this.proof = proof; }

    @Override
    public void fromJson(String val) {
        GsonWrapper gson = new GsonWrapper();
        ResECDH data = gson.fromJson(val, ResECDH.class);
        publicKey = data.publicKey;
        proof = data.proof;
    }
}
