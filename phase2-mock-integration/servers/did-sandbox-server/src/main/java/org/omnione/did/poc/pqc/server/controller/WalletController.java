/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.omnione.did.poc.pqc.server.ResponseMessage;
import org.omnione.did.poc.pqc.server.util.JsonUtils;
import org.omnione.did.poc.pqc.server.datamodel.data.AttestedDidDoc;
import org.omnione.did.poc.pqc.server.service.MockService;
import org.omnione.did.poc.pqc.server.util.MockDataFactory;
import org.omnione.did.poc.pqc.server.util.RandomUtil;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import lombok.Generated;
import org.omnione.did.common.util.JsonUtil;
import org.omnione.did.crypto.enums.MultiBaseType;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.data.model.did.DidDocument;
import org.omnione.did.data.model.did.Proof;
import org.omnione.did.data.model.provider.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/wallet/api/v1"})
public class WalletController {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(WalletController.class);
    private final MockService mockService;

    public WalletController(MockService mockService) {
        this.mockService = mockService;
    }

    @PostMapping(value={"/request-sign-wallet"})
    public String signWallet(@RequestBody String request) {
        try {
            String walletId = RandomUtil.generateWalletId();
            Gson gson = JsonUtils.GSON;
            DidDocument didDoc = gson.fromJson(request, DidDocument.class);
            AttestedDidDoc attestedDidDoc = this.generateAttestedDidDoc(didDoc, walletId);
            return gson.toJson(attestedDidDoc);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AttestedDidDoc generateAttestedDidDoc(DidDocument holderDidDocument, String walletId) {
        try {
            log.debug("\t--> Retrieving Wallet DID Document");
            HashMap<String, Object> response = new HashMap<String, Object>();
            response.put("walletId", walletId);
            response.put("nonce", java.util.UUID.randomUUID().toString().replace("-", ""));
            String encodedDidDocument = MultiBaseUtils.encode(holderDidDocument.toJson().getBytes(StandardCharsets.UTF_8), MultiBaseType.base64url);
            response.put("ownerDidDoc", encodedDidDocument);
            Provider provider = MockDataFactory.createProvider("did:omn:wallet", ResponseMessage.Common.SERVER_URL + "/wallet/api/v1/certificate-vc");
            response.put("provider", provider);
            Proof proof = MockDataFactory.createSignatureProof(MockDataFactory.now(), "did:omn:wallet?versionId=1#assert");
            response.put("proof", proof);
            ObjectMapper objectMapper = new ObjectMapper();
            AttestedDidDoc signatureObject = objectMapper.convertValue(response, AttestedDidDoc.class);
            return this.signAttestedDidDoc(encodedDidDocument, signatureObject, walletId);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public AttestedDidDoc signAttestedDidDoc(String encodedDidDocument, AttestedDidDoc signatureObject, String walletId) {
        try {
            String serializedJson = JsonUtil.serializeAndSort(signatureObject);
            String proofValue = this.mockService.addProof(serializedJson);
            HashMap<String, Object> response = new HashMap<String, Object>();
            response.put("walletId", walletId);
            response.put("nonce", java.util.UUID.randomUUID().toString().replace("-", ""));
            response.put("ownerDidDoc", encodedDidDocument);
            Provider provider = MockDataFactory.createProvider("did:omn:wallet", ResponseMessage.Common.SERVER_URL + "/wallet/api/v1/certificate-vc");
            response.put("provider", provider);
            response.put("did", "did:omn:wallet");
            Proof proof = MockDataFactory.createSignatureProof(MockDataFactory.now(), "did:omn:wallet?versionId=1#assert");
            proof.setProofValue(proofValue);
            response.put("proof", proof);
            ObjectMapper objectMapper = new ObjectMapper();
            AttestedDidDoc attestedDidDoc = objectMapper.convertValue(response, AttestedDidDoc.class);
            return attestedDidDoc;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

