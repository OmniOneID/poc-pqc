/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import org.omnione.did.poc.pqc.server.ResponseMessage;
import org.omnione.did.poc.pqc.server.util.JsonUtils;
import org.omnione.did.poc.pqc.server.service.MockService;
import org.omnione.did.poc.pqc.server.util.MockDataFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Generated;
import org.omnione.did.common.util.JsonUtil;
import org.omnione.did.data.model.did.Proof;
import org.omnione.did.data.model.provider.Provider;
import org.omnione.did.data.model.vc.VerifiableCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping(value={"/cas/api/v1"})
public class CasController {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(CasController.class);
    private final MockService mockService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String USER_DATA_PATH = "src/main/resources/user.json";
    private static final String VALIDATION_API_URL = ResponseMessage.Common.SERVER_URL + "/middleware/api/student/basic-info?studentNo=";

    public CasController(MockService mockService) {
        this.mockService = mockService;
    }

    @PostMapping(value={"/request-wallet-tokendata"}, produces={"application/json"})
    public String requestWalletTokenData(@RequestBody String request) {
        try {
            Map<String, Object> requestMap = this.objectMapper.readValue(request, new TypeReference<Map<String, Object>>(){});
            HashMap<String, Object> responseMap = new HashMap<String, Object>();
            HashMap<String, Object> seed = new HashMap<String, Object>();
            seed.put("purpose", requestMap.get("purpose"));
            seed.put("pkgName", requestMap.get("pkgName"));
            seed.put("nonce", requestMap.get("nonce"));
            seed.put("validUntil", requestMap.get("validUntil"));
            seed.put("userId", requestMap.get("userId"));
            responseMap.put("seed", seed);
            responseMap.put("sha256_pii", "zA2tmT5bnjrsxWY3vhQYm4QhzGtTSDPproNfMNfvmb9A4");
            Provider provider = MockDataFactory.createProvider("did:omn:cas", ResponseMessage.Common.SERVER_URL + "/cas/api/v1/certificate-vc");
            responseMap.put("provider", provider);
            responseMap.put("nonce", MockDataFactory.generateNonce());
            Proof proof = MockDataFactory.createSignatureProof(MockDataFactory.now(), "did:omn:cas?versionId=1#assert");
            responseMap.put("proof", proof);
            Gson gson = JsonUtils.GSON;
            String responseBody = this.objectMapper.writeValueAsString(this.signAttestedData(responseMap));
            return responseBody;
        }
        catch (Exception e) {
            log.error("Error in registerWallet: ", e);
            return "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}";
        }
    }

    public Map<String, Object> signAttestedData(Map<String, Object> responseMap) {
        try {
            // proofValue=null 상태로 직렬화하여 서명
            String serializedJson = JsonUtil.serializeAndSort(responseMap);
            String proofValue = this.mockService.addProof(serializedJson);
            // 동일한 맵에 proofValue만 세팅 (nonce/time 재생성하지 않음)
            Proof proof = (Proof) responseMap.get("proof");
            proof.setProofValue(proofValue);
            return responseMap;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping(value={"/certificate-vc"})
    public String getCertVc() {
        String response2 = this.getCerificateVC();
        return response2;
    }

    public String getCerificateVC() {
        Map<String, String> attribute = new HashMap<String, String>();
        attribute.put("licenseNumber", "");

        VerifiableCredential vc = MockDataFactory.createCertificateVcWithAttribute(
                "did:omn:cas", "o=cas", "AppProvider",
                "did:omn:tas", "tas", "did:omn:tas?versionId=1#assert",
                attribute);

        org.omnione.did.data.model.vc.VcProof proof = (org.omnione.did.data.model.vc.VcProof) vc.getProof();
        proof.setProofValueList(this.mockService.generateProofValueList(vc));

        return this.mockService.signVc(vc);
    }

    @PostMapping(value={"/request-attested-appinfo"})
    public String requestAttestedAppInfo(@RequestBody String request) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> requestMap = objectMapper.readValue(request, new TypeReference<Map<String, Object>>(){});
            HashMap<String, Object> responseMap = new HashMap<String, Object>();
            String appId = requestMap.get("appId").toString();
            responseMap.put("appId", appId);
            Provider provider = MockDataFactory.createProvider("did:omn:cas", ResponseMessage.Common.SERVER_URL + "/cas/api/v1/certificate-vc");
            responseMap.put("provider", provider);
            responseMap.put("nonce", MockDataFactory.generateNonce());
            Proof proof = MockDataFactory.createSignatureProof(MockDataFactory.now(), "did:omn:cas?versionId=1#assert");
            responseMap.put("proof", proof);
            Gson gson = JsonUtils.GSON;
            return gson.toJson(responseMap);
        }
        catch (Exception e) {
            log.error("Error in registerWallet: ", e);
            return "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}";
        }
    }

    @PostMapping(value={"/user/registration/status"})
    public ResponseEntity<String> userStatus(@RequestBody String request) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        File file = new File(USER_DATA_PATH);
        LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
        ArrayList agreements = new ArrayList();
        Gson gson = JsonUtils.GSON;
        if (!file.exists() || file.length() == 0L) {
            responseMap.put("userRegistrationStatus", "NEW");
            LinkedHashMap<String, Object> termsOfService = new LinkedHashMap<String, Object>();
            termsOfService.put("type", "TERMS_OF_SERVICE");
            termsOfService.put("title", "Terms of Service");
            termsOfService.put("url", ResponseMessage.Common.SERVER_URL + "/terms");
            termsOfService.put("required", true);
            agreements.add(termsOfService);
            LinkedHashMap<String, Object> privacyPolicy = new LinkedHashMap<String, Object>();
            privacyPolicy.put("type", "PRIVACY_POLICY");
            privacyPolicy.put("title", "Personal Information Consent");
            privacyPolicy.put("url", ResponseMessage.Common.SERVER_URL + "/privacy");
            privacyPolicy.put("required", true);
            agreements.add(privacyPolicy);
            responseMap.put("agreements", agreements);
            return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(gson.toJson(responseMap));
        }
        try {
            Map<String, Object> requestData = mapper.readValue(request, new TypeReference<Map<String, Object>>(){});
            String userId = (String)requestData.get("userId");
            String walletId = (String)requestData.get("walletId");
            String email = (String)requestData.get("email");
            if (userId == null || walletId == null) {
                responseMap.put("code", "SCRVCFA00000");
                responseMap.put("description", "\uc694\uccad \ud30c\ub77c\ubbf8\ud130\uac00 \uc798\ubabb\ub418\uc5c8\uc2b5\ub2c8\ub2e4. userId\uc640 walletId\ub294 \ud544\uc218\uc785\ub2c8\ub2e4.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(gson.toJson(responseMap));
            }
            String UserValidCode = this.isUserValid(userId, email);
            if (UserValidCode.equals("SCRVCFA01011")) {
                responseMap.put("code", "SCRVCFA01011");
                responseMap.put("description", "No student information found. Please check your Student ID");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(gson.toJson(responseMap));
            }
            if (UserValidCode.equals("SCRVCFA01010")) {
                responseMap.put("code", "SCRVCFA01010");
                responseMap.put("description", "The Student ID or Email is incorrect.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(gson.toJson(responseMap));
            }
            List<Map<String, Object>> users = mapper.readValue(file, new TypeReference<List<Map<String, Object>>>(){});
            Optional<Map<String, Object>> foundUser = users.stream().filter(user -> userId.equals(user.get("userId"))).findFirst();
            if (foundUser.isPresent()) {
                Map user2 = foundUser.get();
                if (userId.equals("tec01")) {
                    responseMap.put("code", "SCRVCFA01009");
                    responseMap.put("description", "This account is locked. Please try again tomorrow.");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(gson.toJson(responseMap));
                }
                if (!walletId.equals(user2.get("walletId"))) {
                    responseMap.put("userRegistrationStatus", "OTHER_DEVICE");
                    LinkedHashMap<String, Object> termsOfService = new LinkedHashMap<String, Object>();
                    termsOfService.put("type", "TERMS_OF_SERVICE");
                    termsOfService.put("title", "Terms of Service");
                    termsOfService.put("url", ResponseMessage.Common.SERVER_URL + "/terms");
                    termsOfService.put("required", true);
                    agreements.add(termsOfService);
                    LinkedHashMap<String, Object> privacyPolicy = new LinkedHashMap<String, Object>();
                    privacyPolicy.put("type", "PRIVACY_POLICY");
                    privacyPolicy.put("title", "Personal Information Consent");
                    privacyPolicy.put("url", ResponseMessage.Common.SERVER_URL + "/privacy");
                    privacyPolicy.put("required", true);
                    agreements.add(privacyPolicy);
                    responseMap.put("agreements", agreements);
                } else {
                    if (!email.equals(user2.get("email"))) {
                        responseMap.put("code", "SCRVCFA01010");
                        responseMap.put("description", "The Student ID or Email is incorrect.");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(gson.toJson(responseMap));
                    }
                    responseMap.put("userRegistrationStatus", "CURRENT");
                }
            } else {
                Optional<Map<String, Object>> foundUserByWallet = users.stream().filter(user -> walletId.equals(user.get("walletId"))).findFirst();
                if (foundUserByWallet.isPresent()) {
                    responseMap.put("userRegistrationStatus", "USED_BY_OTHER");
                } else {
                    responseMap.put("userRegistrationStatus", "NEW");
                    LinkedHashMap<String, Object> termsOfService = new LinkedHashMap<String, Object>();
                    termsOfService.put("type", "TERMS_OF_SERVICE");
                    termsOfService.put("title", "Terms of Service");
                    termsOfService.put("url", ResponseMessage.Common.SERVER_URL + "/terms");
                    termsOfService.put("required", true);
                    agreements.add(termsOfService);
                    LinkedHashMap<String, Object> privacyPolicy = new LinkedHashMap<String, Object>();
                    privacyPolicy.put("type", "PRIVACY_POLICY");
                    privacyPolicy.put("title", "Personal Information Consent");
                    privacyPolicy.put("url", ResponseMessage.Common.SERVER_URL + "/privacy");
                    privacyPolicy.put("required", true);
                    agreements.add(privacyPolicy);
                    responseMap.put("agreements", agreements);
                }
            }
            return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(gson.toJson(responseMap));
        }
        catch (IOException e) {
            responseMap.put("code", "SCRVCFA01009");
            responseMap.put("description", "\uc694\uccad \ucc98\ub9ac \uc911 \uc11c\ubc84 \ub0b4\ubd80 \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4: " + e.getMessage());
            log.error("\uc0ac\uc6a9\uc790 \uc0c1\ud0dc \ud655\uc778 \uc911 \uc624\ub958", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body(gson.toJson(responseMap));
        }
    }

    @PostMapping(value={"/user/otp/request"})
    public String requestOtp(@RequestBody String request) {
        LinkedHashMap<String, Integer> responseMap = new LinkedHashMap<String, Integer>();
        Gson gson = JsonUtils.GSON;
        responseMap.put("expiresIn", 3000);
        return gson.toJson(responseMap);
    }

    @PostMapping(value={"/user/otp/verify-signup"})
    public String verifySignUp(@RequestBody String request) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        HashMap<String, Object> responseMap = new HashMap<String, Object>();
        Gson gson = JsonUtils.GSON;
        try {
            Map<String, Object> requestData = mapper.readValue(request, new TypeReference<Map<String, Object>>(){});
            String userId = (String)requestData.get("userId");
            String email = (String)requestData.get("email");
            if (userId == null || email == null) {
                responseMap.put("message", "\uc694\uccad\uc5d0 userId \ub610\ub294 email \uc815\ubcf4\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return gson.toJson(responseMap);
            }
            boolean isUserAdded = this.updateUserFile(requestData, mapper);
            if (isUserAdded) {
                String opUrl = ResponseMessage.Common.SERVER_URL + "/op/api/v1/jwt/token";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HashMap<String, Object> requestBody = new HashMap<String, Object>();
                requestBody.put("sub", userId);
                requestBody.put("aud", Collections.singletonList("did:omn:cas"));
                HttpEntity entity = new HttpEntity(requestBody, headers);
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<Map> responseEntity = restTemplate.exchange(opUrl, HttpMethod.POST, entity, Map.class, new Object[0]);
                Map opResponse = (Map)responseEntity.getBody();
                if (responseEntity.getStatusCode() == HttpStatus.OK && opResponse != null) {
                    String accessToken = (String)opResponse.get("access_token");
                    String refreshToken = (String)opResponse.get("refresh_token");
                    responseMap.put("access_token", accessToken);
                    responseMap.put("refresh_token", refreshToken);
                    responseMap.put("token_type", "Bearer");
                    responseMap.put("expires_in", 8000);
                }
            } else {
                responseMap.put("description", "\uc774\ubbf8 \ub4f1\ub85d\ub41c \uc0ac\uc6a9\uc790");
                responseMap.put("code", "ERROR");
            }
        }
        catch (IOException e) {
            responseMap.put("message", "\uc694\uccad \ucc98\ub9ac \uc911 \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4: " + e.getMessage());
        }
        return gson.toJson(responseMap);
    }

    private String isUserValid(String userId, String email) {
        HashMap<String, String> resultMap = new HashMap<String, String>();
        RestTemplate restTemplate = new RestTemplate();
        String url = VALIDATION_API_URL + userId;
        ObjectMapper mapper = new ObjectMapper();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, new Object[0]);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> validationData = mapper.readValue((String)response.getBody(), new TypeReference<Map<String, Object>>(){});
                Map studentInfo = (Map)validationData.get("studentInfo");
                if (studentInfo == null) {
                    resultMap.put("code", "SCRVCFA01011");
                    resultMap.put("description", "No student information found. Please check your Student ID");
                    return "SCRVCFA01011";
                }
                String validatedStudentId = (String)studentInfo.get("studentNo");
                String validatedEmail = (String)studentInfo.get("email");
                if (!userId.equals(validatedStudentId)) {
                    resultMap.put("code", "SCRVCFA01011");
                    resultMap.put("description", "No student information found. Please check your Student ID");
                    return "SCRVCFA01011";
                }
                if (!email.equals(validatedEmail)) {
                    resultMap.put("code", "SCRVCFA01010");
                    resultMap.put("description", "The Student ID or Email is incorrect.");
                    return "SCRVCFA01010";
                }
                return "OK";
            }
            log.error("\uc678\ubd80 \ud559\uc0dd \uc815\ubcf4 \uc2dc\uc2a4\ud15c\uc5d0\uc11c \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4. \uc0c1\ud0dc \ucf54\ub4dc: {}", (Object)response.getStatusCode());
            resultMap.put("code", "SCRVCFA00000");
            resultMap.put("description", "\uc678\ubd80 \ud559\uc0dd \uc815\ubcf4 \uc2dc\uc2a4\ud15c\uc5d0\uc11c \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4.");
            return "SCRVCFA00000";
        }
        catch (HttpClientErrorException.NotFound e) {
            log.error("\uc678\ubd80 \uc2dc\uc2a4\ud15c\uc5d0 \uc874\uc7ac\ud558\uc9c0 \uc54a\ub294 \uc0ac\uc6a9\uc790: {}", (Object)e.getMessage());
            log.warn("\uc678\ubd80 \uc2dc\uc2a4\ud15c\uc5d0 \uc874\uc7ac\ud558\uc9c0 \uc54a\ub294 \uc0ac\uc6a9\uc790: {}", (Object)userId);
            resultMap.put("code", "SCRVCFA01011");
            resultMap.put("description", "\uc678\ubd80 \ud559\uc0dd \uc815\ubcf4 \uc2dc\uc2a4\ud15c\uc5d0\uc11c \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4. \uc0c1\ud0dc \ucf54\ub4dc: {}\", response.getStatusCode()");
            return "SCRVCFA01011";
        }
        catch (Exception e) {
            log.error("\uc0ac\uc6a9\uc790 \uac80\uc99d \uc911 \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4: {}", (Object)e.getMessage());
            resultMap.put("code", "SCRVCFA00000");
            resultMap.put("description", "\uc0ac\uc6a9\uc790 \uac80\uc99d \uc911 \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4");
            return "SCRVCFA00000";
        }
    }

    @PostMapping(value={"/user/otp/verify-signin"})
    public String verifySignIn(@RequestBody String request) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        File file = new File(USER_DATA_PATH);
        HashMap<String, Object> responseMap = new HashMap<String, Object>();
        Gson gson = JsonUtils.GSON;
        if (!file.exists() || file.length() == 0L) {
            responseMap.put("message", "\ub85c\uadf8\uc778 \uc2e4\ud328: \ub4f1\ub85d\ub41c \uc0ac\uc6a9\uc790\uac00 \uc5c6\uc74c");
            return gson.toJson(responseMap);
        }
        try {
            Map<String, Object> requestData = mapper.readValue(request, new TypeReference<Map<String, Object>>(){});
            String otp = (String)requestData.get("otp");
            String userId = (String)requestData.get("userId");
            if (otp == null || userId == null) {
                responseMap.put("message", "\uc694\uccad \ud30c\ub77c\ubbf8\ud130\uac00 \uc798\ubabb\ub428.");
                return gson.toJson(responseMap);
            }
            List<Map<String, Object>> users = mapper.readValue(file, new TypeReference<List<Map<String, Object>>>(){});
            Optional<Map<String, Object>> foundUser = users.stream().filter(user -> userId.equals(user.get("userId"))).findFirst();
            if (foundUser.isPresent()) {
                Map user2 = foundUser.get();
                String url = ResponseMessage.Common.SERVER_URL + "/op/api/v1/jwt/token";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HashMap<String, Object> requestBody = new HashMap<String, Object>();
                requestBody.put("sub", userId);
                requestBody.put("aud", Collections.singletonList("did:omc:cas"));
                HttpEntity entity = new HttpEntity(requestBody, headers);
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class, new Object[0]);
                Map opResponse = (Map)responseEntity.getBody();
                if (responseEntity.getStatusCode() == HttpStatus.OK && opResponse != null) {
                    String accessToken = (String)opResponse.get("access_token");
                    String refreshToken = (String)opResponse.get("refresh_token");
                    String tokenType = (String)opResponse.get("token_type");
                    int expires_in = (Integer)opResponse.get("expires_in");
                    responseMap.put("access_token", accessToken);
                    responseMap.put("refresh_token", refreshToken);
                    responseMap.put("token_type", "Bearer");
                    responseMap.put("expires_in", 8000);
                }
            } else {
                responseMap.put("message", "\ub85c\uadf8\uc778 \uc2e4\ud328: userId\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
            }
            return gson.toJson(responseMap);
        }
        catch (IOException e) {
            responseMap.put("message", "\ub85c\uadf8\uc778 \ucc98\ub9ac \uc911 \uc11c\ubc84 \ub0b4\ubd80 \uc624\ub958\uac00 \ubc1c\uc0dd");
            e.printStackTrace();
            return gson.toJson(responseMap);
        }
    }

    @PostMapping(value={"/user/signout"})
    public String signOut(@RequestBody String request) {
        LinkedHashMap responseMap = new LinkedHashMap();
        Gson gson = JsonUtils.GSON;
        return gson.toJson(responseMap);
    }

    @DeleteMapping(value={"/user/withdraw"})
    public ResponseEntity<String> withdrawUser(@RequestHeader(value="Authorization") String authorization, @RequestBody(required=false) String request) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        File file = new File(USER_DATA_PATH);
        LinkedHashMap<String, String> responseMap = new LinkedHashMap<String, String>();
        Gson gson = JsonUtils.GSON;
        String subFromPayload = "";
        try {
            String token;
            String[] parts;
            if (authorization != null && authorization.startsWith("Bearer ") && (parts = (token = authorization.substring(7)).split("\\.")).length >= 2) {
                String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                Map<String, Object> payloadMap = mapper.readValue(payloadJson, new TypeReference<Map<String, Object>>(){});
                subFromPayload = (String)payloadMap.get("sub");
            }
        }
        catch (Exception e) {
            log.error("Failed to extract sub from Authorization header", e);
        }
        if (!file.exists()) {
            responseMap.put("message", "\uc0ac\uc6a9\uc790 \ud30c\uc77c\uc774 \uc874\uc7ac\ud558\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(responseMap));
        }
        try {
            String userId = subFromPayload;
            if (subFromPayload == null) {
                responseMap.put("message", "\uc694\uccad\uc5d0 userId \uc815\ubcf4\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(gson.toJson(responseMap));
            }
            List<Map<String, Object>> users = mapper.readValue(file, new TypeReference<List<Map<String, Object>>>(){});
            boolean userFoundAndRemoved = users.removeIf(user -> userId.equals(user.get("userId")));
            if (userFoundAndRemoved) {
                mapper.writeValue(file, users);
                responseMap.put("message", "\uc0ac\uc6a9\uc790 \ud0c8\ud1f4\uac00 \uc131\uacf5\uc801\uc73c\ub85c \ucc98\ub9ac\ub418\uc5c8\uc2b5\ub2c8\ub2e4.");
                return ResponseEntity.ok(gson.toJson(responseMap));
            }
            responseMap.put("message", "\ud574\ub2f9 \uc0ac\uc6a9\uc790\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(responseMap));
        }
        catch (IOException e) {
            log.error("\uc0ac\uc6a9\uc790 \ud0c8\ud1f4 \ucc98\ub9ac \uc911 \uc624\ub958 \ubc1c\uc0dd", e);
            responseMap.put("message", "\uc11c\ubc84 \ub0b4\ubd80 \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(responseMap));
        }
    }

    @PostMapping(value={"/user/withdraw2"})
    public ResponseEntity<String> withdrawUser2(@RequestBody String request) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        File file = new File(USER_DATA_PATH);
        LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
        Gson gson = JsonUtils.GSON;
        if (!file.exists()) {
            responseMap.put("message", "\uc0ac\uc6a9\uc790 \ud30c\uc77c\uc774 \uc874\uc7ac\ud558\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(responseMap));
        }
        try {
            Map<String, Object> requestData = mapper.readValue(request, new TypeReference<Map<String, Object>>(){});
            String userId = (String)requestData.get("userId");
            if (userId == null) {
                responseMap.put("message", "\uc694\uccad\uc5d0 userId \uc815\ubcf4\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(gson.toJson(responseMap));
            }
            List<Map<String, Object>> users = mapper.readValue(file, new TypeReference<List<Map<String, Object>>>(){});
            boolean userFoundAndRemoved = users.removeIf(user -> userId.equals(user.get("userId")));
            if (userFoundAndRemoved) {
                mapper.writeValue(file, users);
                responseMap.put("message", "\uc0ac\uc6a9\uc790 \ud0c8\ud1f4\uac00 \uc131\uacf5\uc801\uc73c\ub85c \ucc98\ub9ac\ub418\uc5c8\uc2b5\ub2c8\ub2e4.");
                return ResponseEntity.ok(gson.toJson(responseMap));
            }
            log.warn("\ud0c8\ud1f4 \uc694\uccad: \uc0ac\uc6a9\uc790 {}\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.", (Object)userId);
            responseMap.put("message", "\ud574\ub2f9 \uc0ac\uc6a9\uc790\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(responseMap));
        }
        catch (IOException e) {
            log.error("\uc0ac\uc6a9\uc790 \ud0c8\ud1f4 \ucc98\ub9ac \uc911 \uc624\ub958 \ubc1c\uc0dd", e);
            responseMap.put("message", "\uc11c\ubc84 \ub0b4\ubd80 \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(responseMap));
        }
    }

    @PostMapping(value={"/agreement/submit"})
    public ResponseEntity<String> submitAgreement(@RequestBody String request) {
        Gson gson = JsonUtils.GSON;
        HashMap responseMap = new HashMap();
        return ResponseEntity.ok(gson.toJson(responseMap));
    }

    @GetMapping(value={"/agreement/get"})
    public ResponseEntity<String> getAgreement(@RequestParam(value="scope") String scope) {
        Gson gson = JsonUtils.GSON;
        HashMap responseMap = new HashMap();
        ArrayList agreementsList = new ArrayList();
        HashMap<String, Object> agreement1 = new HashMap<String, Object>();
        agreement1.put("type", "TERMS_OF_SERVICE");
        agreement1.put("title", "Terms of service");
        agreement1.put("url", ResponseMessage.Common.SERVER_URL + "/terms");
        agreement1.put("required", true);
        agreementsList.add(agreement1);
        HashMap<String, Object> agreement2 = new HashMap<String, Object>();
        agreement2.put("type", "PRIVACY_POLICY");
        agreement2.put("title", "Privacy Policy");
        agreement2.put("url", ResponseMessage.Common.SERVER_URL + "/privacy");
        agreement2.put("required", true);
        agreementsList.add(agreement2);
        HashMap<String, Object> agreement3 = new HashMap<String, Object>();
        agreement3.put("type", "THIRD_PARTY_PRIVACY");
        agreement3.put("title", "Consent to Third-Party Disclosure");
        agreement3.put("url", ResponseMessage.Common.SERVER_URL + "/privacy");
        agreement3.put("required", true);
        agreementsList.add(agreement3);
        responseMap.put("agreements", agreementsList);
        return ResponseEntity.ok(gson.toJson(responseMap));
    }

    private boolean updateUserFile(Map<String, Object> newUser, ObjectMapper mapper) throws IOException {
        File file = new File(USER_DATA_PATH);
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

    @PostMapping(value={"/jwt/verify"})
    public ResponseEntity<String> verify() {
        HashMap<String, String> responseMap = new HashMap<String, String>();
        responseMap.put("userId", "tec00");
        Gson gson = JsonUtils.GSON;
        return ResponseEntity.status(HttpStatus.OK).body(gson.toJson(responseMap));
    }
}

