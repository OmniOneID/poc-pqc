/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.omnione.did.poc.pqc.server.ResponseMessage;
import org.omnione.did.poc.pqc.server.util.JsonUtils;
import org.omnione.did.poc.pqc.server.util.MockDataFactory;
import org.omnione.did.data.model.vc.CredentialSchema;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.data.model.did.DidDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/lss/api/v1"})
public class LssController {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(LssController.class);
    private static String getUserDidDataPath() {
        return org.omnione.did.poc.pqc.server.util.MockDataInitializer.getUserDidDataPath();
    }

    @GetMapping(value={"/did-doc"})
    public String getDidDoc(@RequestParam(value="did") String did) {
        HashMap response = new HashMap();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            File file = new File(getUserDidDataPath());
            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User data file not found.").toString();
            }
            List<Map<String, Object>> users = objectMapper.readValue(file, new TypeReference<List<Map<String, Object>>>(){});
            for (Map<String, Object> user : users) {
                if (!did.equals(user.get("holderDID"))) continue;
                String ownerDidDoc = (String)user.get("ownerDidDoc");
                byte[] decodedBytes = MultiBaseUtils.decode(ownerDidDoc);
                String decodedDidDoc = new String(decodedBytes, StandardCharsets.UTF_8);
                Map<String, Object> didDocMap = objectMapper.readValue(decodedDidDoc, new TypeReference<Map<String, Object>>(){});
                String jsonResponse = objectMapper.writeValueAsString(didDocMap);
                return jsonResponse;
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("DID not found.").toString();
        }
        catch (IOException | CryptoException e) {
            log.error("Error reading user data file: {}", (Object)e.getMessage(), (Object)e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request.").toString();
        }
    }

    public String getDidDoc() {
        String[] ids;
        HashMap<String, Object> didDoc = new HashMap<String, Object>();
        didDoc.put("@context", Collections.singletonList("https://www.w3.org/ns/did/v1"));
        didDoc.put("assertionMethod", Collections.singletonList("assert"));
        didDoc.put("authentication", Collections.singletonList("auth"));
        didDoc.put("capabilityDelegation", new ArrayList());
        didDoc.put("capabilityInvocation", Collections.singletonList("invoke"));
        didDoc.put("controller", "did:omn:tas");
        didDoc.put("created", MockDataFactory.now());
        didDoc.put("deactivated", false);
        didDoc.put("id", "did:omn:cas");
        didDoc.put("keyAgreement", Collections.singletonList("keyagree"));
        didDoc.put("updated", MockDataFactory.now());
        ArrayList verificationMethods = new ArrayList();
        String pubKey = ResponseMessage.Crypto.PUBLIC_KEY;
        for (String methodId : ids = new String[]{"assert", "auth", "keyagree", "invoke"}) {
            HashMap<String, Object> method = new HashMap<String, Object>();
            method.put("authType", 1);
            method.put("controller", "did:omn:cas");
            method.put("id", methodId);
            // keyagree: 설정에 따라 ML-KEM-768 또는 ECDH(Secp256r1)
            if ("keyagree".equals(methodId)) {
                method.put("publicKeyMultibase", ResponseMessage.Crypto.KA_PUBLIC_KEY);
                method.put("type", org.omnione.did.poc.pqc.server.config.PqcConfig.getKeyAgreementKeyType());
            } else {
                method.put("publicKeyMultibase", pubKey);
                method.put("type", org.omnione.did.poc.pqc.server.config.PqcConfig.getVerificationKeyType());
            }
            verificationMethods.add(method);
        }
        didDoc.put("verificationMethod", verificationMethods);
        didDoc.put("versionId", "1");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DidDocument didDocObj = objectMapper.convertValue(didDoc, DidDocument.class);
        String json = null;
        try {
            json = objectMapper.writeValueAsString(didDoc);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Gson gson = JsonUtils.GSON;
        DidDocument signatureObject = gson.fromJson(json, DidDocument.class);
        return json;
    }

    @GetMapping(value={"/vcplan/list"})
    public String getVcPlanList() {
        HashMap<String, Serializable> responseMap = new HashMap<String, Serializable>();
        responseMap.put("count", Integer.valueOf(1));
        CredentialSchema credentialSchema = MockDataFactory.createCredentialSchema(
                ResponseMessage.Common.SERVER_URL + "/issuer/api/v1/vc/vcschema?name=omnione.student_id",
                "OsdSchemaCredential");
        HashMap<String, Boolean> option = new HashMap<String, Boolean>();
        option.put("allowUserInit", true);
        option.put("allowIssuerInit", false);
        option.put("delegatedIssuance", false);
        HashMap<String, Object> vcPlan = new HashMap<String, Object>();
        vcPlan.put("vcPlanId", "student.user-init");
        vcPlan.put("name", "Student ID VC plan");
        vcPlan.put("description", "Student ID VC plan");
        vcPlan.put("tags", Collections.singletonList("student.user-init"));
        vcPlan.put("credentialSchema", credentialSchema);
        vcPlan.put("option", option);
        vcPlan.put("allowedIssuers", Collections.singletonList("did:omn:issuer"));
        vcPlan.put("manager", "did:omn:issuer");
        ArrayList<HashMap<String, Object>> items = new ArrayList<HashMap<String, Object>>();
        items.add(vcPlan);
        responseMap.put("items", items);
        Gson gson = JsonUtils.GSON;
        return gson.toJson(responseMap);
    }

    private boolean updateUserFile(Map<String, Object> newUser, ObjectMapper mapper) throws IOException {
        File file = new File(getUserDidDataPath());
        file.getParentFile().mkdirs();
        List<Map<String, Object>> users = file.exists() && file.length() > 0L ? mapper.readValue(file, new TypeReference<List<Map<String, Object>>>(){}) : new ArrayList();
        String newUserEmail = (String)newUser.get("email");
        boolean userExists = users.stream().anyMatch(user -> newUserEmail.equals(user.get("email")));
        if (userExists) {
            return false;
        }
        users.add(newUser);
        mapper.writeValue(file, users);
        return true;
    }

    @Generated
    public LssController() {
    }
}

