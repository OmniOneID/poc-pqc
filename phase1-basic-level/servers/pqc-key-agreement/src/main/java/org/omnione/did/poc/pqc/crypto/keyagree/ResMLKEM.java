package org.omnione.did.poc.pqc.crypto.keyagree;

import org.omnione.did.data.model.DataObject;
import org.omnione.did.data.model.did.Proof;
import org.omnione.did.data.model.util.json.GsonWrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * ML-KEM 키 교환 응답 메시지.
 *
 * 기존 ECDH 응답(resECDH)과 대응되는 구조:
 *   resECDH:  { publicKey, proof }  (Bob도 공개키를 보내 양측이 ECDH 수행)
 *   ResMLKEM: { ciphertext, proof } (Bob이 Alice 공개키로 캡슐화한 결과를 전달)
 *
 * serverNonce는 기존과 동일하게 Profile(IssueProcess/VerifyProcess)에서 별도 전달된다.
 *
 * Bob → Alice: "당신의 공개키로 캡슐화했으니, 이 ciphertext를 decapsulate 해"
 */
public class ResMLKEM extends DataObject {

    /** ML-KEM encapsulation으로 생성된 ciphertext (multibase encoded) */
    @SerializedName("ciphertext")
    @Expose
    private String ciphertext;

    /** ML-DSA 서명으로 응답의 무결성·인증 보장 */
    @SerializedName("proof")
    @Expose
    private Proof proof;

    public String getCiphertext() { return ciphertext; }
    public void setCiphertext(String ciphertext) { this.ciphertext = ciphertext; }

    public Proof getProof() { return proof; }
    public void setProof(Proof proof) { this.proof = proof; }

    @Override
    public void fromJson(String val) {
        GsonWrapper gson = new GsonWrapper();
        ResMLKEM data = gson.fromJson(val, ResMLKEM.class);
        ciphertext = data.ciphertext;
        proof = data.proof;
    }
}
