/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.omnione.did.poc.pqc.server.ResponseMessage;
import org.omnione.did.poc.pqc.server.service.MockService;
import org.omnione.did.poc.pqc.server.util.JsonUtils;
import org.omnione.did.poc.pqc.server.util.MockDataFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.omnione.did.crypto.engines.CipherInfo;
import org.omnione.did.crypto.enums.EccCurveType;
import org.omnione.did.crypto.enums.MultiBaseType;
import org.omnione.did.crypto.enums.SymmetricCipherType;
import org.omnione.did.crypto.enums.SymmetricPaddingType;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.util.CryptoUtils;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.data.model.did.Proof;
import org.omnione.did.data.model.profile.Filter;
import org.omnione.did.data.model.profile.ReqE2e;
import org.omnione.did.poc.pqc.server.config.PqcConfig;
import org.omnione.did.data.model.profile.verify.InnerVerifyProfile;
import org.omnione.did.data.model.profile.verify.VerifyProcess;
import org.omnione.did.data.model.profile.verify.VerifyProfile;
import org.omnione.did.data.model.provider.ProviderDetail;
import org.omnione.did.data.model.schema.VcSchema;
import org.omnione.did.data.model.vc.CredentialSchema;
import org.omnione.did.data.model.vc.VerifiableCredential;
import org.omnione.did.poc.pqc.server.util.LogStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/verifier/api/v1"})
public class VerifierController {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(VerifierController.class);
    private final MockService mockService;
    private final LogStreamService logStreamService;

    public VerifierController(MockService mockService, LogStreamService logStreamService) {
        this.mockService = mockService;
        this.logStreamService = logStreamService;
    }

    @PostMapping(value={"/request-offer"})
    public String requestOffer(@RequestBody String request) {
        String offerId = "d2076247-44aa-4a5e-b94a-6bb283adbe03";
        LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
        Gson gson = JsonUtils.GSON;
        LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("offerId", offerId);
        payload.put("type", "VerifyOffer");
        payload.put("mode", "Direct");
        payload.put("device", "WEB");
        payload.put("service", "StudentID.vc");
        payload.put("endpoints", Collections.singletonList(ResponseMessage.Common.SERVER_URL + "/verifier/api/v1/request-verify"));
        payload.put("validUntil", MockDataFactory.nowPlusOneYear());
        payload.put("locked", false);
        responseMap.put("payload", payload);
        responseMap.put("txId", ResponseMessage.Cas.TXID);
        return gson.toJson(responseMap);
    }

    @PostMapping(value={"/request-offer-qr"})
    public String requestOfferQr(@RequestBody String request) {
        String offerId = "d2076247-44aa-4a5e-b94a-6bb283adbe03";
        LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
        Gson gson = JsonUtils.GSON;
        LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("offerId", offerId);
        payload.put("type", "VerifyOffer");
        payload.put("mode", "Direct");
        payload.put("device", "WEB");
        payload.put("service", "StudentID.vc");
        payload.put("endpoints", Collections.singletonList(ResponseMessage.Common.SERVER_URL + "/verifier/api/v1/request-verify"));
        payload.put("validUntil", MockDataFactory.nowPlusOneYear());
        payload.put("locked", false);
        responseMap.put("payload", payload);
        responseMap.put("txId", ResponseMessage.Cas.TXID);
        return gson.toJson(responseMap);
    }

    @PostMapping(value={"/request-profile"})
    public String requestVerifyProfile(@RequestBody String request) {
        try {
            VerifyProfile profile = new VerifyProfile();
            profile.setId("a47328f6-4e52-411f-a04b-358212f5065d");
            profile.setType("VerifyProfile");
            profile.setTitle("StudentID VC Verification");
            profile.setDescription("VP verification profile for Driver's License VC");
            profile.setEncoding("UTF-8");
            profile.setLanguage("ko");

            // verifier
            ProviderDetail verifier = MockDataFactory.createProviderDetail(
                    "did:omn:verifier",
                    ResponseMessage.Common.SERVER_URL + "/verifier/api/v1/certificate-vc",
                    "verifier",
                    ResponseMessage.Common.SERVER_URL + "/verifier"
            );

            // filter
            CredentialSchema schema = new CredentialSchema();
            schema.setId(ResponseMessage.Common.SERVER_URL + "/issuer/api/v1/vc/vcschema?name=omnione.student_id");
            schema.setType("OsdSchemaCredential");
            schema.setValue("meyJ0aXRsZSI6IkxpY2Vuc2UgVkMgRmlsdGVyIiwiaWQiOiJod...");
            schema.setDisplayClaims(Arrays.asList("org.omnione.student_id.name", "org.omnione.student_id.student_id", "org.omnione.student_id.major"));
            schema.setRequiredClaims(Arrays.asList("org.omnione.student_id.name", "org.omnione.student_id.student_id", "org.omnione.student_id.major"));
            schema.setAllowedIssuers(Arrays.asList("did:omn:issuer"));

            Filter filter = new Filter();
            filter.setCredentialSchemas(Collections.singletonList(schema));

            // process
            Object reqE2e = MockDataFactory.createDefaultReqE2eAuto();

            if (PqcConfig.isMlKem768()) {
                // ML-KEM 모드: SDK VerifyProcess에 ReqE2e(algorithm 필드 없음)를 넣을 수 없으므로
                // process를 Map으로 구성하고, innerProfile도 Map으로 구성
                Map<String, Object> processMap = new LinkedHashMap<>();
                processMap.put("endpoints", Arrays.asList(ResponseMessage.Common.SERVER_URL + "/verifier"));
                processMap.put("reqE2e", reqE2e);
                processMap.put("verifierNonce", MockDataFactory.generateNonce());
                processMap.put("authType", 6);

                Map<String, Object> innerProfileMap = new LinkedHashMap<>();
                innerProfileMap.put("verifier", verifier);
                innerProfileMap.put("filter", filter);
                innerProfileMap.put("process", processMap);

                Proof proof = MockDataFactory.createSignatureProof(
                        MockDataFactory.now(),
                        "did:omn:verifier?versionId=1#assert"
                );

                Map<String, Object> profileMap = new LinkedHashMap<>();
                profileMap.put("id", "a47328f6-4e52-411f-a04b-358212f5065d");
                profileMap.put("type", "VerifyProfile");
                profileMap.put("title", "StudentID VC Verification");
                profileMap.put("description", "VP verification profile for Driver's License VC");
                profileMap.put("encoding", "UTF-8");
                profileMap.put("language", "ko");
                profileMap.put("profile", innerProfileMap);
                profileMap.put("proof", proof);

                HashMap<String, Object> responseMap = new HashMap<>();
                responseMap.put("txId", "8b16c70f-1742-4f6f-892e-71717a29056e");
                responseMap.put("profile", profileMap);

                String serializedJson = JsonUtils.serializeAndSort(responseMap);
                String proofValue = this.mockService.addProof(serializedJson);
                proof.setProofValue(proofValue);
                profileMap.put("proof", proof);

                Gson gson = JsonUtils.GSON;
                return gson.toJson(responseMap);
            }

            // ECDH 모드: 기존 SDK 모델 사용
            VerifyProcess process = new VerifyProcess();
            process.setEndpoints(Arrays.asList(ResponseMessage.Common.SERVER_URL + "/verifier"));
            process.setReqE2e((ReqE2e) reqE2e);
            process.setVerifierNonce(MockDataFactory.generateNonce());
            process.setAuthType(6);

            // inner profile
            InnerVerifyProfile innerProfile = new InnerVerifyProfile();
            innerProfile.setVerifier(verifier);
            innerProfile.setFilter(filter);
            innerProfile.setProcess(process);
            profile.setProfile(innerProfile);

            // proof (without proofValue initially, for signing)
            Proof proof = MockDataFactory.createSignatureProof(
                    MockDataFactory.now(),
                    "did:omn:verifier?versionId=1#assert"
            );
            profile.setProof(proof);

            // build response wrapper
            HashMap<String, Object> responseMap = new HashMap<String, Object>();
            responseMap.put("txId", "8b16c70f-1742-4f6f-892e-71717a29056e");
            responseMap.put("profile", profile);

            // sign: serialize, compute proof, set proofValue
            String serializedJson = JsonUtils.serializeAndSort(responseMap);
            String proofValue = this.mockService.addProof(serializedJson);
            proof.setProofValue(proofValue);

            Gson gson = JsonUtils.GSON;
            return gson.toJson(responseMap);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/request-verify"})
    public String requestVerify(@RequestBody String request) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> requestMap = objectMapper.readValue(request, new TypeReference<Map<String, Object>>(){});
            Map accE2e = (Map)requestMap.get("accE2e");
            String iv = (String)accE2e.get("iv");
            String encVp = (String)requestMap.get("encVp");
            // 요청 필드 기반 자동 감지: ciphertext 또는 publicKey 필드로 ML-KEM/ECDH 구분
            String ciphertext = (String)accE2e.get("ciphertext");
            String publicKey = (String)accE2e.get("publicKey");
            String vp;
            if (PqcConfig.isMlKem768()) {
                String ct = (ciphertext != null) ? ciphertext : publicKey;
                if (ct == null) throw new IllegalArgumentException("Missing ciphertext/publicKey in accE2e");
                vp = new String(this.decryptEncVpMlKem(encVp, MultiBaseUtils.decode(iv), ct));
            } else {
                if (publicKey == null) throw new IllegalArgumentException("Missing publicKey in accE2e");
                vp = new String(this.decryptEncVp(encVp, MultiBaseUtils.decode(iv), MultiBaseUtils.decode(publicKey)));
            }
            logStreamService.broadcast("decoded", "[DECODED] POST /verifier/api/v1/request-verify | " + vp);

            // ── 실제 서명 검증 ──
            List<String> verificationErrors = new ArrayList<>();

            // 1) VP outer proof 검증
            String vpResult = org.omnione.did.poc.pqc.server.util.ProofVerifier.verifyVpProof(vp);
            if (vpResult != null) {
                verificationErrors.add("[VP] " + vpResult);
                log.warn("VP proof verification FAILED: {}", vpResult);
            } else {
                log.info("VP proof verification OK");
            }

            // 2) VP 안의 각 VC 검증
            Map<String, Object> vpMap = objectMapper.readValue(vp, new TypeReference<>(){});
            List<Map<String, Object>> vcList = (List<Map<String, Object>>) vpMap.get("verifiableCredential");
            if (vcList != null) {
                for (int i = 0; i < vcList.size(); i++) {
                    Map<String, Object> vc = vcList.get(i);
                    // proofValue 검증
                    String vcResult = org.omnione.did.poc.pqc.server.util.ProofVerifier.verifyVcProof(vc);
                    if (vcResult != null) {
                        verificationErrors.add("[VC#" + i + " proofValue] " + vcResult);
                        log.warn("VC#{} proofValue verification FAILED: {}", i, vcResult);
                    } else {
                        log.info("VC#{} proofValue verification OK", i);
                    }
                    // proofValueList 검증
                    List<String> pvlResults = org.omnione.did.poc.pqc.server.util.ProofVerifier.verifyVcProofValueList(vc);
                    for (int j = 0; j < pvlResults.size(); j++) {
                        if (pvlResults.get(j) != null) {
                            verificationErrors.add("[VC#" + i + " claim#" + j + "] " + pvlResults.get(j));
                            log.warn("VC#{} claim#{} proofValueList verification FAILED: {}", i, j, pvlResults.get(j));
                        } else {
                            log.info("VC#{} claim#{} proofValueList verification OK", i, j);
                        }
                    }
                }
            }

            if (!verificationErrors.isEmpty()) {
                log.error("=== VP/VC Verification FAILED ({} errors) ===", verificationErrors.size());
                verificationErrors.forEach(e -> log.error("  - {}", e));
                // broadcast로 poc.html에도 에러 전달
                logStreamService.broadcast("decoded", "[DECODED] POST /verifier/api/v1/request-verify/result | {\"verified\":false,\"errors\":" + JsonUtils.GSON.toJson(verificationErrors) + "}");
            } else {
                log.info("=== VP/VC Verification ALL PASSED ===");
                logStreamService.broadcast("decoded", "[DECODED] POST /verifier/api/v1/request-verify/result | {\"verified\":true}");
            }

            ResponseMessage.Session.CLAIM = this.resultConfirm(vp);
            return ResponseMessage.Tas.RESPONSE_PROPOSE_REGISTER_USER;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String resultConfirm(String vp) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> requestMap = objectMapper.readValue(vp, new TypeReference<Map<String, Object>>(){});
        HashMap<String, Object> response = new HashMap<String, Object>();
        response.put("result", true);
        ArrayList credentialSubjects = new ArrayList();
        String subjectId = (String)requestMap.get("holder");
        List vcList = (List)requestMap.get("verifiableCredential");
        Map credentialSubject = (Map)((Map)vcList.get(0)).get("credentialSubject");
        credentialSubject.put("id", subjectId);
        response.put("credentialSubject", credentialSubject);
        return JsonUtils.GSON.toJson(response);
    }

    @PostMapping(value={"/confirm-verify"})
    public String confirmRegisterUser(@RequestBody String request) {
        try {
            HashMap<String, Boolean> responseMap = new HashMap<String, Boolean>();
            responseMap.put("result", true);
            return ResponseMessage.Session.CLAIM;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping(value={"/vc/vcschema"})
    public String getVcSchema(@RequestParam(value="name") String name) {
        VcSchema schema = MockDataFactory.createStudentIdVcSchema();
        return schema.toJson();
    }

    @GetMapping(value={"/certificate-vc"})
    public String getCertVc() {
        String response2 = this.getCerificateVC();
        return response2;
    }

    public String getCerificateVC() {
        VerifiableCredential vc = MockDataFactory.createCertificateVc(
                "did:omn:verifier",
                "o=verifier",
                "Verifier",
                "did:omn:tas",
                "tas",
                "did:omn:tas?versionId=1#assert"
        );
        org.omnione.did.data.model.vc.VcProof proof = (org.omnione.did.data.model.vc.VcProof) vc.getProof();
        proof.setProofValueList(this.mockService.generateProofValueList(vc));
        return this.mockService.signVc(vc);
    }

    private byte[] decryptEncVp(String encReqVc, byte[] iv, byte[] clientPublicKey) {
        try {
            byte[] decryptedReqVc = MultiBaseUtils.decode(encReqVc);
            SymmetricPaddingType symmetricPaddingType = SymmetricPaddingType.PKCS5;
            SymmetricCipherType symmetricCipherType = SymmetricCipherType.AES_256_CBC;
            // ECDH는 항상 ECC 키 사용
            byte[] sharedSecret = VerifierController.generateSharedSecret(clientPublicKey, MultiBaseUtils.decode(ResponseMessage.Crypto.ECC_PRIVATE_KEY));
            byte[] mergedSharedSecret = VerifierController.mergeSharedSecretAndNonce(sharedSecret, MultiBaseUtils.decode(ResponseMessage.Crypto.E2E_NONCE_VC));
            ResponseMessage.Session.SESSION_KEY_VC = MultiBaseUtils.encode(mergedSharedSecret, MultiBaseType.base64);
            byte[] sessionKey = MultiBaseUtils.decode(ResponseMessage.Session.SESSION_KEY_VC);
            CipherInfo cipherInfo = new CipherInfo(symmetricCipherType, symmetricPaddingType);
            return CryptoUtils.decrypt(decryptedReqVc, cipherInfo, sessionKey, iv);
        }
        catch (Exception e) {
            log.error("\t--> Exception occurred during decryptEncVp: {}", (Object)e.getMessage(), (Object)e);
            throw new RuntimeException(e);
        }
    }

    private byte[] decryptEncVpMlKem(String encVp, byte[] iv, String ciphertext) {
        try {
            byte[] decryptedVp = MultiBaseUtils.decode(encVp);
            SymmetricPaddingType symmetricPaddingType = SymmetricPaddingType.PKCS5;
            SymmetricCipherType symmetricCipherType = SymmetricCipherType.AES_256_CBC;
            // ML-KEM: decapsulate로 shared secret 획득
            byte[] sharedSecret = org.omnione.did.poc.pqc.server.util.PqcCryptoUtils.mlKemDecapsulate(
                    ResponseMessage.Crypto.MLKEM_DECAP_KEY, ciphertext);
            byte[] mergedSharedSecret = VerifierController.mergeSharedSecretAndNonce(sharedSecret, MultiBaseUtils.decode(ResponseMessage.Crypto.E2E_NONCE_VC));
            ResponseMessage.Session.SESSION_KEY_VC = MultiBaseUtils.encode(mergedSharedSecret, MultiBaseType.base64);
            byte[] sessionKey = MultiBaseUtils.decode(ResponseMessage.Session.SESSION_KEY_VC);
            CipherInfo cipherInfo = new CipherInfo(symmetricCipherType, symmetricPaddingType);
            return CryptoUtils.decrypt(decryptedVp, cipherInfo, sessionKey, iv);
        }
        catch (Exception e) {
            log.error("\t--> Exception occurred during decryptEncVpMlKem: {}", (Object)e.getMessage(), (Object)e);
            throw new RuntimeException(e);
        }
    }

    public static byte[] generateSharedSecret(byte[] publicKey, byte[] privateKey) {
        try {
            return CryptoUtils.generateSharedSecret(publicKey, privateKey, EccCurveType.Secp256r1);
        }
        catch (CryptoException e) {
            log.error("Failed to generate shared secret: {}", (Object)e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static byte[] mergeSharedSecretAndNonce(byte[] sharedSecret, byte[] nonce) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(sharedSecret, 0, sharedSecret.length);
            digest.update(nonce, 0, nonce.length);
            byte[] combinedResult = digest.digest();
            return Arrays.copyOfRange(combinedResult, 0, 32);
        }
        catch (NoSuchAlgorithmException e) {
            log.error("Failed to merge shared secret and nonce: {}", (Object)e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
