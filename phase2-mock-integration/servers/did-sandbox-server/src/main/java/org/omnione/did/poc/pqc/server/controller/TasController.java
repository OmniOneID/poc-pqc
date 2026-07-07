/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.omnione.did.poc.pqc.server.ResponseMessage;
import org.omnione.did.poc.pqc.server.preference.Preference;
import org.omnione.did.poc.pqc.server.service.MockService;
import org.omnione.did.poc.pqc.server.util.JsonUtils;
import org.omnione.did.poc.pqc.server.util.LogStreamService;
import org.omnione.did.poc.pqc.server.util.MockDataFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import org.omnione.did.common.util.JsonUtil;
import org.omnione.did.crypto.engines.CipherInfo;
import org.omnione.did.crypto.enums.DigestType;
import org.omnione.did.crypto.enums.EccCurveType;
import org.omnione.did.crypto.enums.MultiBaseType;
import org.omnione.did.crypto.enums.SymmetricCipherType;
import org.omnione.did.crypto.enums.SymmetricPaddingType;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.util.CryptoUtils;
import org.omnione.did.crypto.util.DigestUtils;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.data.model.did.Proof;
import org.omnione.did.data.model.profile.ReqE2e;
import org.omnione.did.poc.pqc.server.config.PqcConfig;
import org.omnione.did.data.model.provider.Provider;
import org.omnione.did.data.model.provider.ProviderDetail;
import org.omnione.did.data.model.vc.CredentialSchema;
import org.omnione.did.data.model.vc.VerifiableCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/tas/api/v1"})
public class TasController {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(TasController.class);
    private final MockService mockService;
    private final LogStreamService logStreamService;
    private static String getUserDidDataPath() {
        return org.omnione.did.poc.pqc.server.util.MockDataInitializer.getUserDidDataPath();
    }

    public TasController(MockService mockService, LogStreamService logStreamService) {
        this.mockService = mockService;
        this.logStreamService = logStreamService;
    }

    @PostMapping(value={"/request-register-wallet"})
    public String registerWallet(@RequestBody String request) {
        try {
            return ResponseMessage.Tas.RESPONSE_REGISTER_WALLET;
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
        VerifiableCredential vc = MockDataFactory.createCertificateVc(
                "did:omn:tas", "o=tas", "Tas", "did:omn:tas", "tas", "did:omn:tas?versionId=1#assert");
        org.omnione.did.data.model.vc.VcProof proof = (org.omnione.did.data.model.vc.VcProof) vc.getProof();
        proof.setProofValueList(this.mockService.generateProofValueList(vc));
        return this.mockService.signVc(vc);
    }

    @GetMapping(value={"/vc-schema"})
    public String getVcSchema(@RequestParam(value="name") String name) {
        return ResponseMessage.Tas.CERT_VC_SCHEMA;
    }

    @PostMapping(value={"/propose-register-user"})
    public String proposeRegisterUser(@RequestBody String request) {
        try {
            return ResponseMessage.Tas.RESPONSE_PROPOSE_REGISTER_USER;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/request-ecdh"})
    public String requestEcdh(@RequestBody String request) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> requestMap = objectMapper.readValue(request, new TypeReference<Map<String, Object>>(){});
            HashMap<String, Object> responseMap = new HashMap<String, Object>();
            Map reqEcdh = (Map) requestMap.get("reqEcdh");
            if (reqEcdh == null) throw new IllegalArgumentException("Missing reqEcdh");

            String clientNonce = Optional.ofNullable(reqEcdh.get("clientNonce")).map(Object::toString)
                    .orElseThrow(() -> new IllegalArgumentException("Missing clientNonce in reqEcdh"));
            String serverNonce = MockDataFactory.generateNonce();
            byte[] mergedNonce = this.mergeNonces(clientNonce, MultiBaseUtils.decode(serverNonce));

            String clientPublicKeyStr = Optional.ofNullable(reqEcdh.get("publicKey")).map(Object::toString)
                    .orElseThrow(() -> new IllegalArgumentException("Missing publicKey in reqEcdh"));

            byte[] clientPublicKey = MultiBaseUtils.decode(clientPublicKeyStr);
            byte[] sharedSecret = TasController.generateSharedSecret(clientPublicKey, MultiBaseUtils.decode(ResponseMessage.Crypto.ECC_PRIVATE_KEY));

            byte[] mergedSharedSecret = TasController.mergeSharedSecretAndNonce(sharedSecret, mergedNonce);
            ResponseMessage.Session.SESSION_KEY = MultiBaseUtils.encode(mergedSharedSecret, MultiBaseType.base64);

            responseMap.put("txId", "db45edee-950a-4eef-8d0e-091f43d0e22c");
            HashMap<String, Object> accEcdh = new HashMap<String, Object>();
            accEcdh.put("server", "did:omn:tas");
            accEcdh.put("serverNonce", serverNonce);
            accEcdh.put("publicKey", ResponseMessage.Crypto.ECC_PUBLIC_KEY);
            accEcdh.put("cipher", "AES-256-CBC");
            accEcdh.put("padding", "PKCS5");

            Proof proof = MockDataFactory.createProof("Secp256r1Signature2018", MockDataFactory.now(), "did:omn:tas#keyagree", "keyAgreement");
            accEcdh.put("proof", proof);
            proof.setProofValue("mII4wAE5VmS8V6x+DG/+xRGBXrg6JnU0wTTBck23MUlKRdM5MVanX5ZAGSu18yRdwYjPHmdgD1SgxjJsLY9M/kPQ");
            accEcdh.put("proof", proof);
            responseMap.put("accEcdh", accEcdh);
            Gson gson = JsonUtils.GSON;
            return gson.toJson(responseMap);
        }
        catch (Exception e) {
            log.error("Error in requestEcdh: ", e);
            return "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}";
        }
    }

    /**
     * ML-KEM-768 키교환 엔드포인트 (옵션 1: Wallet-as-receiver).
     *
     * 변경 전(legacy)은 "Wallet이 TAS 장기 encapKey로 encap → ciphertext 전송 → 서버 decap" 였음.
     * 옵션 1 흐름:
     *   1) Wallet이 세션별 ephemeral ML-KEM 키쌍을 생성해 자신의 publicKey를 reqMlKem.publicKey 로 전송
     *   2) 서버가 Wallet publicKey에 대해 encapsulate → (ciphertext, sharedSecret)
     *   3) 서버는 ciphertext를 accMlKem.ciphertext 로 응답하고 sharedSecret 로 SESSION_KEY 유도
     *   4) Wallet이 자기 ephemeral sk로 decapsulate → 동일 sharedSecret 복원
     *
     * 요청: { reqMlKem: { algorithm, client, clientNonce, publicKey, cipher, padding, proof } }
     * 응답: { txId, accMlKem: { server, serverNonce, ciphertext, cipher, padding, proof } }
     *
     * 서버의 장기 ML-KEM 키(MLKEM_DECAP_KEY/MLKEM_ENCAP_KEY)는 이 경로에서 더 이상 사용되지 않음.
     */
    @PostMapping(value={"/request-ml-kem"})
    public String requestMlKem(@RequestBody String request) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> requestMap = objectMapper.readValue(request, new TypeReference<Map<String, Object>>(){});

            Map reqMlKem = (Map) requestMap.get("reqMlKem");
            if (reqMlKem == null) throw new IllegalArgumentException("Missing reqMlKem");

            String clientNonce = Optional.ofNullable(reqMlKem.get("clientNonce")).map(Object::toString)
                    .orElseThrow(() -> new IllegalArgumentException("Missing clientNonce"));

            // 옵션 1: 클라이언트가 자기 ephemeral ML-KEM 공개키를 publicKey 필드로 전송
            String clientPublicKey = Optional.ofNullable(reqMlKem.get("publicKey")).map(Object::toString)
                    .orElseThrow(() -> new IllegalArgumentException("Missing publicKey in reqMlKem"));

            String serverNonce = MockDataFactory.generateNonce();
            byte[] mergedNonce = this.mergeNonces(clientNonce, MultiBaseUtils.decode(serverNonce));

            // 서버가 Wallet 공개키에 대해 encapsulate
            org.omnione.did.poc.pqc.server.util.PqcCryptoUtils.MlKemEncapResult encap =
                    org.omnione.did.poc.pqc.server.util.PqcCryptoUtils.mlKemEncapsulate(clientPublicKey);
            byte[] sharedSecret = encap.sharedSecret;
            String ciphertext = encap.ciphertextEncoded;

            byte[] mergedSharedSecret = TasController.mergeSharedSecretAndNonce(sharedSecret, mergedNonce);
            ResponseMessage.Session.SESSION_KEY = MultiBaseUtils.encode(mergedSharedSecret, MultiBaseType.base64);

            HashMap<String, Object> responseMap = new HashMap<String, Object>();
            responseMap.put("txId", "db45edee-950a-4eef-8d0e-091f43d0e22c");

            HashMap<String, Object> accMlKem = new HashMap<String, Object>();
            accMlKem.put("server", "did:omn:tas");
            accMlKem.put("serverNonce", serverNonce);
            accMlKem.put("ciphertext", ciphertext);                // 옵션 1 신규 필드
            accMlKem.put("cipher", "AES-256-CBC");
            accMlKem.put("padding", "PKCS5");

            Proof proof = MockDataFactory.createProof(
                    "MlDsa44Signature2024", MockDataFactory.now(),
                    "did:omn:tas#keyagree", "keyAgreement");
            accMlKem.put("proof", proof);
            String canonical = org.omnione.did.common.util.JsonUtil.serializeAndSort(accMlKem);
            proof.setProofValue(org.omnione.did.poc.pqc.server.util.PqcCryptoUtils.sign(
                    ResponseMessage.Crypto.MLDSA_PRIVATE_KEY,
                    canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            accMlKem.put("proof", proof);
            responseMap.put("accMlKem", accMlKem);

            return JsonUtils.GSON.toJson(responseMap);
        }
        catch (Exception e) {
            log.error("Error in requestMlKem: ", e);
            return "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}";
        }
    }

    private byte[] mergeNonces(String encodedClientNonce, byte[] serverNonce) {
        try {
            byte[] clientNonce = MultiBaseUtils.decode(encodedClientNonce);
            byte[] combinedNonce = new byte[serverNonce.length + clientNonce.length];
            System.arraycopy(clientNonce, 0, combinedNonce, 0, clientNonce.length);
            System.arraycopy(serverNonce, 0, combinedNonce, clientNonce.length, serverNonce.length);
            return DigestUtils.getDigest(combinedNonce, DigestType.SHA256);
        }
        catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
        catch (CryptoException e) {
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

    @PostMapping(value={"/request-create-token"})
    public String requestCreateToken(@RequestBody String request) {
        try {
            log.debug("\t--> Validating Server token seed");
            log.debug("\t--> Retrieving token expiration date and time");
            log.debug("\t--> Generating Server token data");
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> requestMap = objectMapper.readValue(request, new TypeReference<Map<String, Object>>(){});
            HashMap<String, Object> responseMap = new HashMap<String, Object>();
            Map seed = (Map)requestMap.get("seed");
            Map caAppInfo = (Map)seed.get("caAppInfo");
            String appId = (String)caAppInfo.get("appId");
            Map walletInfo = (Map)seed.get("walletInfo");
            Map wallet = (Map)walletInfo.get("wallet");
            String waleltId = (String)wallet.get("id");
            int purpose = (Integer)seed.get("purpose");
            responseMap.put("appId", appId);
            responseMap.put("nonce", MockDataFactory.generateNonce());
            responseMap.put("purpose", purpose);
            responseMap.put("validUntil", MockDataFactory.nowPlusOneYear());
            responseMap.put("walletId", waleltId);
            Provider provider = MockDataFactory.createProvider(
                    "did:omn:tas",
                    ResponseMessage.Common.SERVER_URL + "/tas/api/v1/certificate-vc");
            responseMap.put("provider", provider);
            Proof proof = MockDataFactory.createSignatureProof(
                    MockDataFactory.now(),
                    "did:omn:tas?versionId=1#assert");
            proof.setProofValue("mIIe5FKIMTW4UjWUOw4QSIA/XdjBBO4Qv1ItyfiG3Sgh9UNqcOWWoq8NQaEyHQrT7750WIDr+U+P3/75tUnqxHQ0");
            responseMap.put("proof", proof);
            Gson gson = JsonUtils.GSON;
            String jsonString = JsonUtil.serializeAndSort(responseMap);
            byte[] serverTokenBytes = DigestUtils.getDigest(jsonString.getBytes(StandardCharsets.UTF_8), DigestType.SHA256);
            String encodedServerToken = MultiBaseUtils.encode(serverTokenBytes, MultiBaseType.base64);
            byte[] ivBytes = CryptoUtils.generateNonce(16);
            String encodedIv = MultiBaseUtils.encode(ivBytes, MultiBaseType.base64);
            if (ResponseMessage.Session.SESSION_KEY == null || ResponseMessage.Session.SESSION_KEY.isEmpty()) {
                throw new IllegalStateException("SESSION_KEY is not set. /request-ecdh must be called before /request-create-token");
            }
            byte[] encryptedServerTokenDataBytes = this.encryptServerTokenData(jsonString, ivBytes, ResponseMessage.Session.SESSION_KEY);
            String encodedEncryptedStd = MultiBaseUtils.encode(encryptedServerTokenDataBytes, MultiBaseType.base64);
            log.debug("*** Finished requestCreateToken ***");
            HashMap<String, String> encResponseMap = new HashMap<String, String>();
            encResponseMap.put("txId", "db45edee-950a-4eef-8d0e-091f43d0e22c");
            encResponseMap.put("iv", encodedIv);
            encResponseMap.put("encStd", encodedEncryptedStd);
            return gson.toJson(encResponseMap);
        }
        catch (Exception e) {
            log.error("\t--> Exception occurred during requestCreateToken: {}", (Object)e.getMessage(), (Object)e);
            throw new RuntimeException(e);
        }
    }

    private byte[] encryptServerTokenData(String stdJson, byte[] iv, String sKey) {
        try {
            SymmetricPaddingType symmetricPaddingType = SymmetricPaddingType.PKCS5;
            SymmetricCipherType symmetricCipherType = SymmetricCipherType.AES_256_CBC;
            byte[] sessionKey = MultiBaseUtils.decode(sKey);
            CipherInfo cipherInfo = new CipherInfo(symmetricCipherType, symmetricPaddingType);
            return CryptoUtils.encrypt(stdJson.getBytes(StandardCharsets.UTF_8), cipherInfo, sessionKey, iv);
        }
        catch (Exception e) {
            log.error("\t--> Exception occurred during encryptServerTokenData: {}", (Object)e.getMessage(), (Object)e);
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/retrieve-kyc"})
    public String retrieveKyc(@RequestBody String request) {
        try {
            return ResponseMessage.Tas.RESPONSE_PROPOSE_REGISTER_USER;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/request-register-user"})
    public ResponseEntity<String> requestRegisterUser(@RequestBody String request) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> requestMap = objectMapper.readValue(request, new TypeReference<Map<String, Object>>(){});
            Map signedDidDoc = (Map)requestMap.get("signedDidDoc");
            String ownerDidDoc = (String)signedDidDoc.get("ownerDidDoc");
            String holderDiddoc = new String(MultiBaseUtils.decode(ownerDidDoc));
            Map<String, Object> holderDiddocMap = objectMapper.readValue(holderDiddoc, new TypeReference<Map<String, Object>>(){});
            String holderDID = (String)holderDiddocMap.get("id");
            HashMap<String, Object> newUser = new HashMap<String, Object>();
            newUser.put("holderDID", holderDID);
            newUser.put("ownerDidDoc", ownerDidDoc);
            if (!this.updateUserFile(newUser, objectMapper)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("User with this DID already exists.");
            }
            Preference.put("id", holderDID);
            return ResponseEntity.ok(ResponseMessage.Tas.RESPONSE_PROPOSE_REGISTER_USER);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/confirm-register-user"})
    public String confirmRegisterUser(@RequestBody String request) {
        try {
            return ResponseMessage.Tas.RESPONSE_PROPOSE_REGISTER_USER;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/update-push-token"})
    public String updatePushToken(@RequestBody String request) {
        try {
            return "";
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/propose-issue-vc"})
    public String proposeIssueVc(@RequestBody String request) {
        try {
            HashMap<String, String> responseMap = new HashMap<String, String>();
            responseMap.put("txId", ResponseMessage.Tas.RESPONSE_PROPOSE_REGISTER_USER);
            responseMap.put("refId", "TNQMxiWPVTuFpAjWl");
            Gson gson = JsonUtils.GSON;
            return gson.toJson(responseMap);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/request-issue-profile"})
    public String requestIssueProfile(@RequestBody String request) {
        try {
            HashMap<String, Object> responseMap = new HashMap<String, Object>();
            ProviderDetail issuer = MockDataFactory.createProviderDetail(
                    "did:omn:issuer",
                    ResponseMessage.Common.SERVER_URL + "/issuer/api/v1/certificate-vc",
                    "issuer",
                    null);
            CredentialSchema credentialSchema = MockDataFactory.createCredentialSchema(
                    ResponseMessage.Common.SERVER_URL + "/issuer/api/v1/vc/vcschema?name=omnione.student_id",
                    "OsdSchemaCredential");
            Object reqE2e = MockDataFactory.createDefaultReqE2eAuto();
            HashMap<String, Object> process = new HashMap<String, Object>();
            process.put("issuerNonce", MockDataFactory.generateNonce());
            process.put("endpoints", Collections.singletonList(ResponseMessage.Common.SERVER_URL));
            process.put("reqE2e", reqE2e);
            HashMap<String, Object> profileInner = new HashMap<String, Object>();
            profileInner.put("issuer", issuer);
            profileInner.put("credentialSchema", credentialSchema);
            profileInner.put("process", process);
            Proof proof = MockDataFactory.createSignatureProof(
                    MockDataFactory.now(),
                    "did:omn:issuer?versionId=1#assert");
            HashMap<String, Object> profile = new HashMap<String, Object>();
            profile.put("profile", profileInner);
            profile.put("proof", proof);
            profile.put("id", "c45aee05-e9b4-4cbc-81bc-e04f1acbf557");
            profile.put("type", "IssueProfile");
            profile.put("title", "Student ID VC plan");
            profile.put("description", "Student ID VC plan");
            profile.put("encoding", "UTF-8");
            profile.put("language", "ko");
            responseMap.put("txId", ResponseMessage.Cas.TXID);
            responseMap.put("authNonce", ResponseMessage.Tas.AUTH_NONCE);
            responseMap.put("profile", profile);
            Gson gson = JsonUtils.GSON;
            String serializedJson = JsonUtils.serializeAndSort(responseMap);
            String proofValue = this.mockService.addProof(serializedJson);
            proof.setProofValue(proofValue);
            profile.put("proof", proof);
            responseMap.put("profile", profile);
            return gson.toJson(responseMap);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/request-issue-vc"})
    public String requestIssueVc(@RequestBody String request) {
        try {
            log.debug("\t--> Generating VC");
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> requestMap = objectMapper.readValue(request, new TypeReference<Map<String, Object>>(){});
            Map accE2e = (Map)requestMap.get("accE2e");
            String iv = (String)accE2e.get("iv");
            Map didAuth = (Map)requestMap.get("didAuth");
            String authNonce = (String)didAuth.get("authNonce");
            String encReqVc = (String)requestMap.get("encReqVc");
            if (!authNonce.equals(ResponseMessage.Tas.AUTH_NONCE)) {
                throw new RuntimeException();
            }
            // 요청 필드 기반 자동 감지: ciphertext 또는 publicKey 필드로 ML-KEM/ECDH 구분
            String ciphertext = (String)accE2e.get("ciphertext");
            String publicKey = (String)accE2e.get("publicKey");
            String reqVc;
            if (PqcConfig.isMlKem768()) {
                // ML-KEM: ciphertext 필드 우선, 없으면 publicKey 필드를 ciphertext로 사용
                String ct = (ciphertext != null) ? ciphertext : publicKey;
                if (ct == null) throw new IllegalArgumentException("Missing ciphertext/publicKey in accE2e");
                reqVc = new String(this.decryptEncReqVcMlKem(encReqVc, MultiBaseUtils.decode(iv), ct));
            } else {
                // ECDH: publicKey로 ECDH shared secret 생성
                if (publicKey == null) throw new IllegalArgumentException("Missing publicKey in accE2e");
                reqVc = new String(this.decryptEncReqVc(encReqVc, MultiBaseUtils.decode(iv), MultiBaseUtils.decode(publicKey)));
            }
            String vcId = UUID.randomUUID().toString();
            String holderDid = Preference.get("id");
            VerifiableCredential vc = MockDataFactory.createStudentIdVc(vcId, holderDid);
            vc.getProof().setProofValueList(this.mockService.generateProofValueList(vc));
            String signedVcJson = this.mockService.signVc(vc);
            HashMap<String, Object> vcMap = objectMapper.readValue(signedVcJson, new TypeReference<HashMap<String, Object>>(){});
            HashMap<String, HashMap<String, Object>> credInfo = new HashMap<String, HashMap<String, Object>>();
            credInfo.put("vc", vcMap);
            String signedCredInfo = JsonUtils.serializeAndSort(credInfo);
            logStreamService.broadcast("decoded", "[DECODED] POST /tas/api/v1/request-issue-vc | " + signedCredInfo);
            byte[] ivBytes = CryptoUtils.generateNonce(16);
            String encodedIv = MultiBaseUtils.encode(ivBytes, MultiBaseType.base64);
            byte[] encryptedVcBytes = this.encryptServerTokenData(signedCredInfo, ivBytes, ResponseMessage.Session.SESSION_KEY_VC);
            String encodedEncryptedVc = MultiBaseUtils.encode(encryptedVcBytes, MultiBaseType.base64);
            log.debug("*** Finished issue VC ***");
            HashMap<String, Object> encResponseMap = new HashMap<String, Object>();
            encResponseMap.put("txId", "db45edee-950a-4eef-8d0e-091f43d0e22c");
            HashMap<String, String> e2e = new HashMap<String, String>();
            e2e.put("iv", encodedIv);
            e2e.put("encVc", encodedEncryptedVc);
            encResponseMap.put("e2e", e2e);
            Gson gson = JsonUtils.GSON;
            return gson.toJson(encResponseMap);
        }
        catch (Exception e) {
            log.error("\t--> Exception occurred during requestCreateToken: {}", (Object)e.getMessage(), (Object)e);
            throw new RuntimeException(e);
        }
    }

    private byte[] decryptEncReqVc(String encReqVc, byte[] iv, byte[] clientPublicKey) {
        try {
            byte[] decryptedReqVc = MultiBaseUtils.decode(encReqVc);
            SymmetricPaddingType symmetricPaddingType = SymmetricPaddingType.PKCS5;
            SymmetricCipherType symmetricCipherType = SymmetricCipherType.AES_256_CBC;
            // ECDH는 항상 ECC 키 사용
            byte[] sharedSecret = TasController.generateSharedSecret(clientPublicKey, MultiBaseUtils.decode(ResponseMessage.Crypto.ECC_PRIVATE_KEY));
            byte[] mergedSharedSecret = TasController.mergeSharedSecretAndNonce(sharedSecret, MultiBaseUtils.decode(ResponseMessage.Crypto.E2E_NONCE_VC));
            ResponseMessage.Session.SESSION_KEY_VC = MultiBaseUtils.encode(mergedSharedSecret, MultiBaseType.base64);
            byte[] sessionKey = MultiBaseUtils.decode(ResponseMessage.Session.SESSION_KEY_VC);
            CipherInfo cipherInfo = new CipherInfo(symmetricCipherType, symmetricPaddingType);
            return CryptoUtils.decrypt(decryptedReqVc, cipherInfo, sessionKey, iv);
        }
        catch (Exception e) {
            log.error("\t--> Exception occurred during decryptEncReqVc: {}", (Object)e.getMessage(), (Object)e);
            throw new RuntimeException(e);
        }
    }

    private byte[] decryptEncReqVcMlKem(String encReqVc, byte[] iv, String ciphertext) {
        try {
            byte[] decryptedReqVc = MultiBaseUtils.decode(encReqVc);
            SymmetricPaddingType symmetricPaddingType = SymmetricPaddingType.PKCS5;
            SymmetricCipherType symmetricCipherType = SymmetricCipherType.AES_256_CBC;
            // ML-KEM: decapsulate로 shared secret 획득
            byte[] sharedSecret = org.omnione.did.poc.pqc.server.util.PqcCryptoUtils.mlKemDecapsulate(
                    ResponseMessage.Crypto.MLKEM_DECAP_KEY, ciphertext);
            byte[] mergedSharedSecret = TasController.mergeSharedSecretAndNonce(sharedSecret, MultiBaseUtils.decode(ResponseMessage.Crypto.E2E_NONCE_VC));
            ResponseMessage.Session.SESSION_KEY_VC = MultiBaseUtils.encode(mergedSharedSecret, MultiBaseType.base64);
            byte[] sessionKey = MultiBaseUtils.decode(ResponseMessage.Session.SESSION_KEY_VC);
            CipherInfo cipherInfo = new CipherInfo(symmetricCipherType, symmetricPaddingType);
            return CryptoUtils.decrypt(decryptedReqVc, cipherInfo, sessionKey, iv);
        }
        catch (Exception e) {
            log.error("\t--> Exception occurred during decryptEncReqVcMlKem: {}", (Object)e.getMessage(), (Object)e);
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/confirm-issue-vc"})
    public String confirmIssueVc(@RequestBody String request) {
        try {
            return ResponseMessage.Tas.RESPONSE_PROPOSE_REGISTER_USER;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/propose-update-diddoc"})
    public String proposeUpdateDiddoc(@RequestBody String request) {
        try {
            HashMap<String, String> responseMap = new HashMap<String, String>();
            responseMap.put("txId", ResponseMessage.Cas.TXID);
            responseMap.put("authNonce", ResponseMessage.Tas.AUTH_NONCE);
            Gson gson = JsonUtils.GSON;
            return gson.toJson(responseMap);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/request-update-diddoc"})
    public ResponseEntity<String> requestUpdateDiddoc(@RequestBody String request) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> requestMap = objectMapper.readValue(request, new TypeReference<Map<String, Object>>(){});
            Map signedDidDoc = (Map)requestMap.get("signedDidDoc");
            String ownerDidDoc = (String)signedDidDoc.get("ownerDidDoc");
            String holderDiddoc = new String(MultiBaseUtils.decode(ownerDidDoc));
            Map<String, Object> holderDiddocMap = objectMapper.readValue(holderDiddoc, new TypeReference<Map<String, Object>>(){});
            String holderDID = (String)holderDiddocMap.get("id");
            HashMap<String, Object> updateUser = new HashMap<String, Object>();
            updateUser.put("holderDID", holderDID);
            updateUser.put("ownerDidDoc", ownerDidDoc);
            if (!this.updateUserDidDocFile(updateUser, objectMapper)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User with this DID not found.");
            }
            return ResponseEntity.ok(ResponseMessage.Tas.RESPONSE_PROPOSE_REGISTER_USER);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/confirm-update-diddoc"})
    public String confirmUpdateDiddoc(@RequestBody String request) {
        try {
            return ResponseMessage.Tas.RESPONSE_PROPOSE_REGISTER_USER;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/propose-revoke-vc"})
    public String proposeRevokeVc(@RequestBody String request) {
        try {
            String txId = java.util.UUID.randomUUID().toString();
            HashMap<String, Object> responseMap = new HashMap<String, Object>();
            responseMap.put("txId", txId);
            // 클라이언트(walletApi.proposeRevokeVc)가 파싱하는 추가 필드:
            //   - issuerNonce: 후속 단계에서 사용할 nonce (multibase 인코딩 16바이트)
            //   - authType   : 인증 방식. 2 = PIN (PinActivity 진입 후 revokeVcProcess까지 검증 가능)
            //                  1(FREE)은 클라이언트 분기 누락으로 멈춤. 0(ANY)은 select-auth 화면 진입.
            responseMap.put("issuerNonce", MockDataFactory.generateNonce());
            responseMap.put("authType", 2);
            Gson gson = JsonUtils.GSON;
            return gson.toJson(responseMap);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * VC 폐기 요청.
     * 클라이언트(walletApi.requestRevokeVc)는 응답에서 txId, issuerNonce를 사용한다.
     * mock 구현이므로 실제 VC 폐기/서명 검증은 수행하지 않고 후속 nonce만 반환한다.
     */
    @PostMapping(value={"/request-revoke-vc"})
    public String requestRevokeVc(@RequestBody String request) {
        try {
            HashMap<String, Object> responseMap = new HashMap<String, Object>();
            responseMap.put("txId", ResponseMessage.Cas.TXID);
            responseMap.put("issuerNonce", MockDataFactory.generateNonce());
            return JsonUtils.GSON.toJson(responseMap);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * VC 폐기 확인.
     * 클라이언트는 응답 본문을 파싱하지 않고 HTTP 200 여부만 확인한다.
     */
    @PostMapping(value={"/confirm-revoke-vc"})
    public String confirmRevokeVc(@RequestBody String request) {
        try {
            HashMap<String, Object> responseMap = new HashMap<String, Object>();
            responseMap.put("txId", ResponseMessage.Cas.TXID);
            return JsonUtils.GSON.toJson(responseMap);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Entity Enrollment Endpoints
    // ═══════════════════════════════════════════════════════════════

    @PostMapping(value={"/propose-enroll-entity"})
    public String proposeEnrollEntity(@RequestBody String request) {
        try {
            String txId = java.util.UUID.randomUUID().toString();
            String authNonce = MultiBaseUtils.encode(
                    org.omnione.did.crypto.util.CryptoUtils.generateNonce(16), MultiBaseType.base64);
            // authNonce를 세션에 저장 (request-enroll-entity에서 검증용)
            ResponseMessage.Tas.AUTH_NONCE = authNonce;

            HashMap<String, Object> responseMap = new HashMap<String, Object>();
            responseMap.put("txId", txId);
            responseMap.put("authNonce", authNonce);
            return JsonUtils.GSON.toJson(responseMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/request-enroll-entity"})
    public String requestEnrollEntity(@RequestBody String request) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> requestMap = objectMapper.readValue(request, new TypeReference<Map<String, Object>>(){});
            String txId = (String) requestMap.get("txId");

            // DID Auth 검증은 mock이므로 생략, certificate VC를 생성하여 암호화 반환
            // 요청에서 DID 추출
            Map didAuth = (Map) requestMap.get("didAuth");
            String entityDid = (String) didAuth.get("did");

            // Certificate VC 생성 (entity 용)
            VerifiableCredential vc = MockDataFactory.createCertificateVc(
                    entityDid, "o=" + entityDid.substring(entityDid.lastIndexOf(':') + 1), "Entity",
                    "did:omn:tas", "tas", "did:omn:tas?versionId=1#assert");
            org.omnione.did.data.model.vc.VcProof vcProof = (org.omnione.did.data.model.vc.VcProof) vc.getProof();
            vcProof.setProofValueList(this.mockService.generateProofValueList(vc));
            String signedVcJson = this.mockService.signVc(vc);

            // 세션키로 암호화
            byte[] ivBytes = org.omnione.did.crypto.util.CryptoUtils.generateNonce(16);
            String encodedIv = MultiBaseUtils.encode(ivBytes, MultiBaseType.base64);
            byte[] sessionKey = MultiBaseUtils.decode(ResponseMessage.Session.SESSION_KEY);
            CipherInfo cipherInfo = new CipherInfo(
                    SymmetricCipherType.AES_256_CBC,
                    SymmetricPaddingType.PKCS5);
            byte[] encryptedVcBytes = org.omnione.did.crypto.util.CryptoUtils.encrypt(
                    signedVcJson.getBytes(java.nio.charset.StandardCharsets.UTF_8), cipherInfo, sessionKey, ivBytes);
            String encodedEncVc = MultiBaseUtils.encode(encryptedVcBytes, MultiBaseType.base64);

            // 서버 로그에 복호화된 VC 남기기 (inspect 용)
            logStreamService.broadcast("decoded", "[DECODED] POST /tas/api/v1/request-enroll-entity | " + signedVcJson);

            HashMap<String, Object> responseMap = new HashMap<String, Object>();
            responseMap.put("txId", txId);
            responseMap.put("encVc", encodedEncVc);
            responseMap.put("iv", encodedIv);
            return JsonUtils.GSON.toJson(responseMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/confirm-enroll-entity"})
    public String confirmEnrollEntity(@RequestBody String request) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> requestMap = objectMapper.readValue(request, new TypeReference<Map<String, Object>>(){});

            return "{}";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean updateUserDidDocFile(Map<String, Object> updateUser, ObjectMapper mapper) throws IOException {
        File file = new File(getUserDidDataPath());
        if (!file.exists() || file.length() <= 0L) {
            return false;
        }
        List<Map<String, Object>> users = mapper.readValue(file, new TypeReference<List<Map<String, Object>>>(){});
        String holderDID = (String)updateUser.get("holderDID");
        boolean userFound = false;
        for (Map<String, Object> user : users) {
            if (!holderDID.equals(user.get("holderDID"))) continue;
            user.put("ownerDidDoc", updateUser.get("ownerDidDoc"));
            userFound = true;
            break;
        }
        if (userFound) {
            mapper.writeValue(file, users);
            return true;
        }
        return false;
    }

    private boolean updateUserFile(Map<String, Object> newUser, ObjectMapper mapper) throws IOException {
        File file = new File(getUserDidDataPath());
        file.getParentFile().mkdirs();
        List<Map<String, Object>> users = file.exists() && file.length() > 0L ? mapper.readValue(file, new TypeReference<List<Map<String, Object>>>(){}) : new ArrayList();
        String holderDID = (String)newUser.get("holderDID");
        boolean userExists = users.stream().anyMatch(user -> holderDID.equals(user.get("holderDID")));
        if (userExists) {
            return false;
        }
        users.add(newUser);
        mapper.writeValue(file, users);
        return true;
    }

}
