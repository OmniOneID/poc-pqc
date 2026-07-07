/*
 * Copyright 2024-2025 OmniOne.
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

package org.omnione.did.sdk.datamodel.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.omnione.did.sdk.datamodel.common.Proof;
import org.omnione.did.sdk.datamodel.common.ProofContainer;
import org.omnione.did.sdk.datamodel.common.enums.SymmetricCipherType;
import org.omnione.did.sdk.datamodel.common.enums.SymmetricPaddingType;
import org.omnione.did.sdk.datamodel.util.IntEnumAdapterFactory;
import org.omnione.did.sdk.datamodel.util.JsonSortUtil;
import org.omnione.did.sdk.datamodel.util.StringEnumAdapterFactory;

import java.util.List;

/**
 * ML-KEM-768 key agreement request (Option 1: Wallet-as-receiver).
 *
 * The client generates an ephemeral ML-KEM-768 key pair per session and
 * transmits its public key here. The server performs ML-KEM.Encap against
 * this ephemeral public key, returns the resulting ciphertext in AccMlKem,
 * and the client decapsulates locally with the ephemeral private key to
 * recover the shared secret. This avoids depending on TAS's long-term
 * ML-KEM key and eliminates the pre-lookup of TAS DID Doc.
 */
public class ReqMlKem implements ProofContainer {
    @SerializedName("client")
    @Expose
    String client;

    @SerializedName("clientNonce")
    @Expose
    String clientNonce;

    @SerializedName("algorithm")
    @Expose
    String algorithm; // "ML-KEM-768"

    @SerializedName("publicKey")
    @Expose
    String publicKey; // Client's ephemeral ML-KEM-768 public key
                     // (X.509 SubjectPublicKeyInfo, multibase-encoded, ~1184B)

    @SerializedName("cipher")
    @Expose
    SymmetricCipherType.SYMMETRIC_CIPHER_TYPE cipher;

    @SerializedName("padding")
    @Expose
    SymmetricPaddingType.SYMMETRIC_PADDING_TYPE padding;

    @SerializedName("proof")
    @Expose
    Proof proof; // KeyAgreeProof (signed with ML-DSA-44)

    @SerializedName("proofs")
    @Expose
    private List<Proof> proofs;

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getClientNonce() {
        return clientNonce;
    }

    public void setClientNonce(String clientNonce) {
        this.clientNonce = clientNonce;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public SymmetricCipherType.SYMMETRIC_CIPHER_TYPE getCipher() {
        return cipher;
    }

    public void setCipher(SymmetricCipherType.SYMMETRIC_CIPHER_TYPE cipher) {
        this.cipher = cipher;
    }

    public SymmetricPaddingType.SYMMETRIC_PADDING_TYPE getPadding() {
        return padding;
    }

    public void setPadding(SymmetricPaddingType.SYMMETRIC_PADDING_TYPE padding) {
        this.padding = padding;
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
