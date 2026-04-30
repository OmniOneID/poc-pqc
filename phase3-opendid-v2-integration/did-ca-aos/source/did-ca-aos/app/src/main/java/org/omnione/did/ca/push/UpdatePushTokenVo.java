package org.omnione.did.ca.push;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.omnione.did.sdk.datamodel.common.Proof;
import org.omnione.did.sdk.datamodel.common.ProofContainer;
import org.omnione.did.sdk.datamodel.token.Wallet;
import org.omnione.did.sdk.datamodel.util.IntEnumAdapterFactory;
import org.omnione.did.sdk.datamodel.util.JsonSortUtil;
import org.omnione.did.sdk.datamodel.util.StringEnumAdapterFactory;

import java.util.List;

public class UpdatePushTokenVo implements ProofContainer {
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("did")
    @Expose
    private String did;
    @SerializedName("appId")
    @Expose
    private String appId;
    @SerializedName("pushToken")
    @Expose
    private String pushToken;
    @SerializedName("proof")
    @Expose
    private Proof proof;
    @SerializedName("proofs")
    @Expose
    private List<Proof> proofs;

    public UpdatePushTokenVo() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDid() {
        return did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPushToken() {
        return pushToken;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public Proof getProof() {
        return proof;
    }

    public void setProof(Proof proof) {
        this.proof = proof;
    }

    public List<Proof> getProofs() {
        return proofs;
    }

    public void setProofs(List<Proof> proofs) {
        this.proofs = proofs;
    }

    @Override
    public String toJson() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new IntEnumAdapterFactory())
                .registerTypeAdapterFactory(new StringEnumAdapterFactory())
                .disableHtmlEscaping()
                .create();
        String json = gson.toJson(this);
        return JsonSortUtil.sortJsonString(gson, json);
    }

    @Override
    public void fromJson(String val) {

    }
}
