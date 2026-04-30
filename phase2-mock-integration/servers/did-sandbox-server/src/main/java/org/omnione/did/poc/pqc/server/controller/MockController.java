/*
 * Decompiled with CFR 0.152.
 */
package org.omnione.did.poc.pqc.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.omnione.did.poc.pqc.server.ResponseMessage;
import org.omnione.did.poc.pqc.server.util.JsonUtils;
import org.omnione.did.poc.pqc.server.service.MockService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.omnione.did.crypto.enums.DigestType;
import org.omnione.did.crypto.enums.EccCurveType;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.util.DigestUtils;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.crypto.util.SignatureUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.omnione.did.poc.pqc.server.util.LogStreamService;

@Controller
public class MockController {
    private final MockService mockService;
    private final ObjectMapper objectMapper;
    private final LogStreamService logStreamService;

    public MockController(MockService mockService, ObjectMapper objectMapper, LogStreamService logStreamService) {
        this.mockService = mockService;
        this.objectMapper = objectMapper;
        this.logStreamService = logStreamService;
    }

    @GetMapping(value={"/"})
    public String poc() {
        return "poc";
    }

    @GetMapping(value={"/api/log-stream"}, produces="text/event-stream")
    @ResponseBody
    public SseEmitter logStream() {
        return logStreamService.subscribe();
    }

    @GetMapping(value={"/api/log-blacklist"})
    @ResponseBody
    public java.util.Set<String> getBlacklist() {
        return logStreamService.getBlacklist();
    }

    @PostMapping(value={"/api/log-blacklist"})
    @ResponseBody
    public java.util.Set<String> addBlacklist(@RequestBody Map<String, String> body) {
        logStreamService.addBlacklist(body.get("uri"));
        return logStreamService.getBlacklist();
    }

    @PostMapping(value={"/api/log-blacklist/remove"})
    @ResponseBody
    public java.util.Set<String> removeBlacklist(@RequestBody Map<String, String> body) {
        logStreamService.removeBlacklist(body.get("uri"));
        return logStreamService.getBlacklist();
    }

    @GetMapping(value={"/demo"})
    public String demo(Model model) {
        return "demo";
    }

    @GetMapping(value={"/tec"})
    public String tec(Model model) {
        return "tec";
    }

    @GetMapping(value={"/terms"})
    public String terms(Model model) {
        return "terms";
    }

    @GetMapping(value={"/privacy"})
    public String privacy(Model model) {
        return "privacy";
    }

    @GetMapping(value={"/dataportal-main2"})
    public String dataPortalMain2(Model model) {
        return "dataportal-main2";
    }

    @GetMapping(value={"/dataportal-main"})
    public String dataPortalMain(Model model) {
        return "dataportal-main";
    }

    @GetMapping(value={"/academic-transcript"})
    public String academicTranscript(Model model) {
        return "academic-transcript";
    }

    @GetMapping(value={"/issue-academic-transcript"})
    public String issueAcademicTranscript(Model model) {
        return "issue-academic-transcript";
    }

    @GetMapping(value={"/result-academic-transcript"})
    public String resultAcademicTranscript(Model model) {
        return "result-academic-transcript";
    }

    @GetMapping(value={"/diddoc"})
    public String diddoc(@RequestParam(name="holderDID", required=false) String holderDID, Model model) {
        block4: {
            try {
                InputStream inputStream = new java.io.FileInputStream(org.omnione.did.poc.pqc.server.util.MockDataInitializer.getUserDidDataPath());
                List<Map<String, String>> userDids = this.objectMapper.readValue(inputStream, new TypeReference<List<Map<String, String>>>(){});
                ArrayList<String> holderDIDs = new ArrayList<String>();
                for (Map<String, String> userDid : userDids) {
                    holderDIDs.add(userDid.get("holderDID"));
                }
                model.addAttribute("holderDIDs", holderDIDs);
                if (holderDID == null) break block4;
                for (Map<String, String> userDid : userDids) {
                    if (!holderDID.equals(userDid.get("holderDID"))) continue;
                    model.addAttribute("selectedDidDoc", userDid.get("ownerDidDoc"));
                    model.addAttribute("selectedHolderDID", holderDID);
                    break;
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "diddoc";
    }

    @GetMapping(value={"/verify"})
    public String verify(Model model) {
        return "verify";
    }

    @GetMapping(value={"/jwt"})
    public String jwt(Model model) {
        return "jwt";
    }

    @GetMapping(value={"/data"})
    public String data(Model model) {
        String savingPeriodUrl = ResponseMessage.Common.SERVER_URL + "/dataportal/api/v1/getSavingPeriod";
        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(savingPeriodUrl, String.class, new Object[0]);
            Map<String, String> responseMap = this.objectMapper.readValue(response, new TypeReference<Map<String, String>>(){});
            String savingPeriod = responseMap.get("savingPeriod");
            model.addAttribute("savingPeriod", savingPeriod);
        }
        catch (Exception e) {
            model.addAttribute("savingPeriod", "5");
        }
        return "data";
    }

    @GetMapping(value={"/vp-agreement"})
    public String vpAgreement(Model model) {
        return "vp-agreement";
    }

    @GetMapping(value={"/addUserInfo"})
    public String addUserInfo(Model model) {
        return "addUserInfo";
    }

    @PostMapping(value={"/verify"})
    @ResponseBody
    public Map<String, Object> verifySignature(@RequestBody Map<String, String> payload) {
        String originalData = payload.get("originalData");
        String publicKey = payload.get("publicKey");
        String signature = payload.get("signature");
        boolean isValid = false;
        Object message = "";
        try {
            if (org.omnione.did.poc.pqc.server.config.PqcConfig.isMlDsa44()) {
                org.omnione.did.poc.pqc.server.util.PqcCryptoUtils.verify(publicKey,
                        originalData.getBytes(StandardCharsets.UTF_8), signature);
            } else {
                byte[] hashedSignOrignData = DigestUtils.getDigest(originalData.getBytes(StandardCharsets.UTF_8), DigestType.SHA256);
                byte[] signatureByte = MultiBaseUtils.decode(signature);
                byte[] compressPublicKeyBytes = MultiBaseUtils.decode(publicKey);
                SignatureUtils.verifyCompactSignWithCompressedKey(compressPublicKeyBytes, hashedSignOrignData, signatureByte, EccCurveType.Secp256r1);
            }
            isValid = true;
            message = "Signature is valid.";
        }
        catch (CryptoException e) {
            isValid = false;
            message = "Signature is invalid: " + e.getMessage();
        }
        catch (Exception e) {
            isValid = false;
            message = "An unexpected error occurred: " + e.getMessage();
        }
        HashMap<String, Object> response = new HashMap<String, Object>();
        response.put("isValid", isValid);
        response.put("message", message);
        return response;
    }

    @GetMapping(value={"/addVcInfo"})
    public String addVcInfo(@RequestParam String did, @RequestParam String userName, @RequestParam String vcSchemaId, Model model) {
        model.addAttribute("did", did);
        model.addAttribute("userName", userName);
        model.addAttribute("vcSchemaId", vcSchemaId);
        return "addVcInfo";
    }

    @PostMapping(value={"/jwt"})
    @ResponseBody
    public Map<String, Object> verifyJwt(@RequestBody Map<String, String> payload) {
        String[] parts;
        String jwt = payload.get("jwt");
        String publicKey = payload.get("publicKey");
        HashMap<String, Object> response = new HashMap<String, Object>();
        HashMap<String, Object> signatureResponse = new HashMap<String, Object>();
        String header = "";
        String claims = "";
        try {
            parts = jwt.split("\\.");
            if (parts.length >= 2) {
                header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
                claims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            }
        }
        catch (Exception e) {
            signatureResponse.put("isValid", false);
            signatureResponse.put("message", "Error decoding JWT: " + e.getMessage());
        }
        response.put("header", header);
        response.put("payload", claims);
        try {
            parts = jwt.split("\\.");
            String originalData = parts[0] + "." + parts[1];
            String signature = parts[2];
            boolean isValid = false;
            Object message = "";
            try {
                if (org.omnione.did.poc.pqc.server.config.PqcConfig.isMlDsa44()) {
                    // ML-DSA-44: Base64URL decode → MultiBase encode → verify
                    byte[] signatureByte = Base64.getUrlDecoder().decode(signature);
                    String signatureMultibase = org.omnione.did.crypto.util.MultiBaseUtils.encode(
                            signatureByte, org.omnione.did.crypto.enums.MultiBaseType.base64);
                    org.omnione.did.poc.pqc.server.util.PqcCryptoUtils.verify(publicKey,
                            originalData.getBytes(StandardCharsets.UTF_8), signatureMultibase);
                } else {
                    byte[] hashedSignOrignData = DigestUtils.getDigest(originalData.getBytes(StandardCharsets.UTF_8), DigestType.SHA256);
                    byte[] signatureByte = Base64.getUrlDecoder().decode(signature);
                    byte[] compressPublicKeyBytes = MultiBaseUtils.decode(publicKey);
                    SignatureUtils.verifyCompactSignWithCompressedKey(compressPublicKeyBytes, hashedSignOrignData, signatureByte, EccCurveType.Secp256r1);
                }
                isValid = true;
                message = "Signature is valid.";
            }
            catch (CryptoException e) {
                isValid = false;
                message = "Signature is invalid: " + e.getMessage();
            }
            catch (Exception e) {
                isValid = false;
                message = "An unexpected error occurred: " + e.getMessage();
            }
            signatureResponse.put("isValid", isValid);
            signatureResponse.put("message", message);
        }
        catch (Exception e) {
            signatureResponse.put("isValid", false);
            signatureResponse.put("message", "Error verifying signature: " + e.getMessage());
        }
        response.put("signature", signatureResponse);
        return response;
    }

    @PostMapping(value={"/diddoc/decode"})
    @ResponseBody
    public Map<String, String> decodeDidDoc(@RequestBody Map<String, String> payload) {
        String holderDID = payload.get("holderDID");
        HashMap<String, String> response = new HashMap<String, String>();
        try {
            InputStream inputStream = new java.io.FileInputStream(org.omnione.did.poc.pqc.server.util.MockDataInitializer.getUserDidDataPath());
            List<Map<String, String>> userDids = this.objectMapper.readValue(inputStream, new TypeReference<List<Map<String, String>>>(){});
            if (holderDID != null) {
                for (Map<String, String> userDid : userDids) {
                    if (!holderDID.equals(userDid.get("holderDID"))) continue;
                    String ownerDidDoc = userDid.get("ownerDidDoc");
                    byte[] decodedBytes = MultiBaseUtils.decode(ownerDidDoc);
                    String decodedString = new String(decodedBytes);
                    response.put("decodedDidDoc", decodedString);
                    return response;
                }
            }
        }
        catch (IOException | CryptoException e) {
            e.printStackTrace();
            response.put("error", "Error decoding Base64: " + e.getMessage());
            return response;
        }
        response.put("error", "holderDID not found");
        return response;
    }

    @GetMapping(value={"/cas/more/links"})
    public ResponseEntity<String> moreLink(@RequestParam(value="os") String os) {
        Gson gson = JsonUtils.GSON;
        HashMap responseMap = new HashMap();
        HashMap<String, Object> linkData = new HashMap<String, Object>();
        linkData.put("title", "Terms of service");
        linkData.put("url", ResponseMessage.Common.SERVER_URL + "/terms");
        ArrayList<HashMap<String, Object>> linksList = new ArrayList<HashMap<String, Object>>();
        linksList.add(linkData);
        responseMap.put("links", linksList);
        return ResponseEntity.ok(gson.toJson(responseMap));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Entity Registration (enrollment SDK 1~5단계 시뮬레이션)
    // ═══════════════════════════════════════════════════════════════

    @PostMapping(value={"/api/entity-registration"})
    @ResponseBody
    public ResponseEntity<Map<String, Object>> entityRegistration(@RequestBody Map<String, String> payload) {
        String entityDid = payload.getOrDefault("entityDid", "did:omn:testentity");
        String entityName = payload.getOrDefault("entityName", "TestEntity");

        RestTemplate restTemplate = new RestTemplate();
        String tasBase = ResponseMessage.Common.SERVER_URL + "/tas/api/v1";
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> steps = new ArrayList<>();

        try {
            // ── Step 1: Register DID to TAS (admin endpoint) ──
            String tasAdminBase = ResponseMessage.Common.SERVER_URL + "/tas/admin/v1";

            // 간이 DID document 생성 (mock용)
            String publicKey = ResponseMessage.Crypto.PUBLIC_KEY;
            String keyType = org.omnione.did.poc.pqc.server.config.PqcConfig.getVerificationKeyType();
            String didDocJson = buildSimpleDidDocJson(entityDid, publicKey, keyType);
            String encodedDidDoc = MultiBaseUtils.encode(
                    didDocJson.getBytes(StandardCharsets.UTF_8),
                    org.omnione.did.crypto.enums.MultiBaseType.base58btc);

            Map<String, Object> step0Req = new HashMap<>();
            step0Req.put("didDoc", encodedDidDoc);
            step0Req.put("name", entityName);
            step0Req.put("serverUrl", ResponseMessage.Common.SERVER_URL);
            step0Req.put("certificateUrl", ResponseMessage.Common.SERVER_URL + "/tas/api/v1/certificate-vc");
            step0Req.put("role", "ISSUER");

            restTemplate.postForObject(tasAdminBase + "/entities/register-did/public",
                    new org.springframework.http.HttpEntity<>(JsonUtils.GSON.toJson(step0Req),
                            jsonHeaders()), String.class);

            steps.add(Map.of("step", 1, "name", "register-did/public", "status", "OK"));
            Thread.sleep(500);

            // ── Step 2: Propose Enroll Entity ──
            Map<String, Object> step1Req = new HashMap<>();
            step1Req.put("id", System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8));

            String step1Res = restTemplate.postForObject(tasBase + "/propose-enroll-entity",
                    new org.springframework.http.HttpEntity<>(JsonUtils.GSON.toJson(step1Req),
                            jsonHeaders()), String.class);
            Map<String, Object> step1Data = objectMapper.readValue(step1Res, new TypeReference<>(){});
            String txId = (String) step1Data.get("txId");
            String authNonce = (String) step1Data.get("authNonce");

            steps.add(Map.of("step", 2, "name", "propose-enroll-entity", "status", "OK", "txId", txId));
            Thread.sleep(500);

            // ── Step 3: Key Exchange (ECDH 또는 ML-KEM) ──
            String clientNonce = MultiBaseUtils.encode(
                    org.omnione.did.crypto.util.CryptoUtils.generateNonce(16),
                    org.omnione.did.crypto.enums.MultiBaseType.base64);

            String now = java.time.Instant.now().atOffset(java.time.ZoneOffset.UTC)
                    .format(java.time.format.DateTimeFormatter.ISO_INSTANT);

            byte[] sharedSecret;
            Map<String, Object> reqEcdh = new java.util.LinkedHashMap<>();
            reqEcdh.put("client", entityDid);
            reqEcdh.put("clientNonce", clientNonce);

            // Option 1: ephemeral ML-KEM private key is generated now but used
            // only after the server response (for decap). Hoisted across branches.
            org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters ephemeralMlKemPriv = null;

            if (org.omnione.did.poc.pqc.server.config.PqcConfig.isMlKem768()) {
                // Option 1 (Wallet-as-receiver):
                //   1) 클라이언트가 세션별 ephemeral ML-KEM-768 키쌍을 생성
                //   2) publicKey(ephemeral)를 REQ에 실어 전송 — TAS 장기 encapKey 불필요
                //   3) 서버가 accMlKem.ciphertext 로 encap 결과를 돌려주면, 그걸 decap 해서 sharedSecret 획득 (응답 처리 블록)
                org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator kemGen =
                        new org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator();
                kemGen.init(new org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters(
                        new java.security.SecureRandom(),
                        org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters.ml_kem_768));
                org.bouncycastle.crypto.AsymmetricCipherKeyPair ephKeyPair = kemGen.generateKeyPair();

                ephemeralMlKemPriv = (org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters) ephKeyPair.getPrivate();
                org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters ephPubParams =
                        (org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters) ephKeyPair.getPublic();

                org.bouncycastle.asn1.x509.SubjectPublicKeyInfo pubInfo =
                        org.bouncycastle.pqc.crypto.util.SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(ephPubParams);
                byte[] ephPubBytes = pubInfo.getEncoded();
                String ephPubEncoded = MultiBaseUtils.encode(ephPubBytes, org.omnione.did.crypto.enums.MultiBaseType.base64);

                reqEcdh.put("algorithm", "ML-KEM-768");
                reqEcdh.put("publicKey", ephPubEncoded);
                reqEcdh.put("candidate", Map.of("ciphers", List.of("AES-256-CBC")));

                // proof: ML-DSA-44로 서명 (ephemeral pk의 진위 보증)
                Map<String, Object> kaProof = new java.util.LinkedHashMap<>();
                kaProof.put("type", org.omnione.did.poc.pqc.server.config.PqcConfig.getKeyAgreementProofType());
                kaProof.put("proofPurpose", "keyAgreement");
                kaProof.put("created", now);
                kaProof.put("verificationMethod", entityDid + "?versionId=1#keyagree");
                reqEcdh.put("proof", kaProof);

                String canonical = org.omnione.did.common.util.JsonUtil.serializeAndSort(reqEcdh);
                String proofValue = mockService.addProof(canonical);
                kaProof.put("proofValue", proofValue);

                sharedSecret = null; // 서버 응답에서 ciphertext 받은 뒤 decap으로 채워짐
            } else {
                // ECDH: 클라이언트 임시 EC 키 쌍 생성
                java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("EC");
                kpg.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
                java.security.KeyPair ecKeyPair = kpg.generateKeyPair();

                java.security.interfaces.ECPublicKey ecPub = (java.security.interfaces.ECPublicKey) ecKeyPair.getPublic();
                byte[] xBytes = ecPub.getW().getAffineX().toByteArray();
                byte[] compressedPub = new byte[33];
                compressedPub[0] = (byte)(ecPub.getW().getAffineY().testBit(0) ? 0x03 : 0x02);
                if (xBytes.length >= 32) {
                    System.arraycopy(xBytes, xBytes.length - 32, compressedPub, 1, 32);
                } else {
                    System.arraycopy(xBytes, 0, compressedPub, 33 - xBytes.length, xBytes.length);
                }
                String clientPubKey = MultiBaseUtils.encode(compressedPub, org.omnione.did.crypto.enums.MultiBaseType.base58btc);

                reqEcdh.put("curve", "Secp256r1");
                reqEcdh.put("publicKey", clientPubKey);
                reqEcdh.put("candidate", Map.of("ciphers", List.of("AES-256-CBC")));

                Map<String, Object> ecdhProof = new java.util.LinkedHashMap<>();
                ecdhProof.put("type", "Secp256r1Signature2018");
                ecdhProof.put("proofPurpose", "keyAgreement");
                ecdhProof.put("created", now);
                ecdhProof.put("verificationMethod", entityDid + "?versionId=1#keyagree");
                reqEcdh.put("proof", ecdhProof);

                String ecdhCanonical = org.omnione.did.common.util.JsonUtil.serializeAndSort(reqEcdh);
                String ecdhProofValue = mockService.addProofEcc(ecdhCanonical);
                ecdhProof.put("proofValue", ecdhProofValue);

                // ECDH shared secret은 응답 후 계산 (아래에서)
                sharedSecret = null; // placeholder
                // ecKeyPair은 아래에서 사용
                // 임시 변수에 저장
                reqEcdh.put("_ecKeyPair", ecKeyPair); // 내부용, JSON 직렬화 전에 제거
            }

            // ecKeyPair 임시 저장 꺼내기 (ECDH용)
            java.security.KeyPair ecKeyPairForEcdh = null;
            if (!org.omnione.did.poc.pqc.server.config.PqcConfig.isMlKem768()) {
                ecKeyPairForEcdh = (java.security.KeyPair) reqEcdh.remove("_ecKeyPair");
            }

            Map<String, Object> step2Req = new HashMap<>();
            step2Req.put("id", System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8));
            step2Req.put("txId", txId);

            String step2Endpoint;
            String accFieldName;
            if (org.omnione.did.poc.pqc.server.config.PqcConfig.isMlKem768()) {
                step2Req.put("reqMlKem", reqEcdh);
                step2Endpoint = "/request-ml-kem";
                accFieldName = "accMlKem";
            } else {
                step2Req.put("reqEcdh", reqEcdh);
                step2Endpoint = "/request-ecdh";
                accFieldName = "accEcdh";
            }

            String step2Res = restTemplate.postForObject(tasBase + step2Endpoint,
                    new org.springframework.http.HttpEntity<>(JsonUtils.GSON.toJson(step2Req),
                            jsonHeaders()), String.class);
            Map<String, Object> step2Data = objectMapper.readValue(step2Res, new TypeReference<>(){});
            Map accEcdh = (Map) step2Data.get(accFieldName);
            if (accEcdh == null) {
                throw new IllegalStateException("Step2 response missing '" + accFieldName + "': " + step2Res);
            }
            String serverNonce = (String) accEcdh.get("serverNonce");

            if (!org.omnione.did.poc.pqc.server.config.PqcConfig.isMlKem768()) {
                // ECDH: 서버 공개키로 shared secret 생성
                String serverPubKey = (String) accEcdh.get("publicKey");
                byte[] serverPubKeyBytes = MultiBaseUtils.decode(serverPubKey);
                sharedSecret = org.omnione.did.crypto.util.CryptoUtils.generateSharedSecret(
                        serverPubKeyBytes, ecKeyPairForEcdh.getPrivate().getEncoded(), EccCurveType.Secp256r1);
            } else {
                // Option 1 ML-KEM: 서버가 encap한 ciphertext를 받아 ephemeral sk로 decap
                String ciphertext = (String) accEcdh.get("ciphertext");
                if (ciphertext == null) {
                    throw new IllegalStateException("Step2 ML-KEM response missing 'ciphertext': " + step2Res);
                }
                byte[] ctBytes = MultiBaseUtils.decode(ciphertext);
                if (ephemeralMlKemPriv == null) {
                    throw new IllegalStateException("Ephemeral ML-KEM private key was not initialized");
                }
                org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor extractor =
                        new org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor(ephemeralMlKemPriv);
                sharedSecret = extractor.extractSecret(ctBytes);
                // NOTE: 실서비스에서는 ephemeralMlKemPriv의 내부 바이트를 zeroize 해야 하지만
                // BC의 MLKEMPrivateKeyParameters는 raw 배열에 직접 접근할 인터페이스를 공개하지 않아
                // 여기서는 GC에 맡긴다. 운영 코드라면 참조를 즉시 null화하는 정도가 최소 조치.
                ephemeralMlKemPriv = null;
            }

            // nonce 병합
            byte[] clientNonceBytes = MultiBaseUtils.decode(clientNonce);
            byte[] serverNonceBytes = MultiBaseUtils.decode(serverNonce);
            byte[] combinedNonce = new byte[clientNonceBytes.length + serverNonceBytes.length];
            System.arraycopy(clientNonceBytes, 0, combinedNonce, 0, clientNonceBytes.length);
            System.arraycopy(serverNonceBytes, 0, combinedNonce, clientNonceBytes.length, serverNonceBytes.length);
            byte[] mergedNonce = DigestUtils.getDigest(combinedNonce, DigestType.SHA256);

            // 세션키 = SHA256(sharedSecret || mergedNonce), 32바이트
            byte[] secretAndNonce = new byte[sharedSecret.length + mergedNonce.length];
            System.arraycopy(sharedSecret, 0, secretAndNonce, 0, sharedSecret.length);
            System.arraycopy(mergedNonce, 0, secretAndNonce, sharedSecret.length, mergedNonce.length);
            byte[] sessionKey = DigestUtils.getDigest(secretAndNonce, DigestType.SHA256);

            steps.add(Map.of("step", 3, "name", "request-ecdh", "status", "OK"));
            Thread.sleep(500);

            // ── Step 4: DID Auth → Request Enroll Entity ──
            String now4 = java.time.Instant.now().atOffset(java.time.ZoneOffset.UTC)
                    .format(java.time.format.DateTimeFormatter.ISO_INSTANT);
            Map<String, Object> authProof = new java.util.LinkedHashMap<>();
            authProof.put("type", org.omnione.did.poc.pqc.server.config.PqcConfig.getProofType());
            authProof.put("proofPurpose", "authentication");
            authProof.put("created", now4);
            authProof.put("verificationMethod", entityDid + "?versionId=1#auth");

            Map<String, Object> didAuth = new java.util.LinkedHashMap<>();
            didAuth.put("authNonce", authNonce);
            didAuth.put("did", entityDid);
            didAuth.put("proof", authProof);

            // proof 서명: proofValue=null 상태로 직렬화 후 서명
            String authCanonical = org.omnione.did.common.util.JsonUtil.serializeAndSort(didAuth);
            String authProofValue = mockService.addProof(authCanonical);
            authProof.put("proofValue", authProofValue);

            Map<String, Object> step3Req = new HashMap<>();
            step3Req.put("id", System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8));
            step3Req.put("txId", txId);
            step3Req.put("didAuth", didAuth);

            String step3Res = restTemplate.postForObject(tasBase + "/request-enroll-entity",
                    new org.springframework.http.HttpEntity<>(JsonUtils.GSON.toJson(step3Req),
                            jsonHeaders()), String.class);
            Map<String, Object> step3Data = objectMapper.readValue(step3Res, new TypeReference<>(){});
            String encVc = (String) step3Data.get("encVc");
            String iv = (String) step3Data.get("iv");

            steps.add(Map.of("step", 4, "name", "request-enroll-entity", "status", "OK"));
            Thread.sleep(500);

            byte[] ivBytes = MultiBaseUtils.decode(iv);
            byte[] encVcBytes = MultiBaseUtils.decode(encVc);
            org.omnione.did.crypto.engines.CipherInfo cipherInfo = new org.omnione.did.crypto.engines.CipherInfo(
                    org.omnione.did.crypto.enums.SymmetricCipherType.AES_256_CBC,
                    org.omnione.did.crypto.enums.SymmetricPaddingType.PKCS5);
            byte[] decryptedVc = org.omnione.did.crypto.util.CryptoUtils.decrypt(
                    encVcBytes, cipherInfo, sessionKey, ivBytes);
            String vcJson = new String(decryptedVc, StandardCharsets.UTF_8);

            // VC 파싱하여 vcId 추출
            Map<String, Object> vcMap = objectMapper.readValue(vcJson, new TypeReference<>(){});
            String vcId = (String) vcMap.get("id");

            // ── Step 5: Confirm Enrollment ──
            Map<String, Object> step5Req = new HashMap<>();
            step5Req.put("id", System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8));
            step5Req.put("txId", txId);
            step5Req.put("vcId", vcId);

            restTemplate.postForObject(tasBase + "/confirm-enroll-entity",
                    new org.springframework.http.HttpEntity<>(JsonUtils.GSON.toJson(step5Req),
                            jsonHeaders()), String.class);

            steps.add(Map.of("step", 5, "name", "confirm-enroll-entity", "status", "OK"));

            result.put("status", "SUCCESS");
            result.put("txId", txId);
            result.put("entityDid", entityDid);
            result.put("steps", steps);
            result.put("didDocument", objectMapper.readValue(didDocJson, new TypeReference<Map<String, Object>>(){}));
            result.put("certificateVc", vcMap);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            result.put("steps", steps);
            return ResponseEntity.status(500).body(result);
        }
    }

    private org.springframework.http.HttpHeaders jsonHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }

    private String buildSimpleDidDocJson(String did, String publicKey, String keyType) {
        Map<String, Object> doc = new java.util.LinkedHashMap<>();
        doc.put("@context", List.of("https://www.w3.org/ns/did/v1"));
        doc.put("assertionMethod", List.of("assert"));
        doc.put("authentication", List.of("auth"));
        doc.put("controller", "did:omn:tas");
        doc.put("created", java.time.Instant.now().toString());
        doc.put("deactivated", false);
        doc.put("id", did);
        doc.put("keyAgreement", List.of("keyagree"));
        doc.put("updated", java.time.Instant.now().toString());
        List<Map<String, Object>> methods = new ArrayList<>();
        for (String id : List.of("assert", "auth", "keyagree")) {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("authType", 1);
            m.put("controller", did);
            m.put("id", id);
            // keyagree: 설정에 따라 ML-KEM-768 또는 ECDH(Secp256r1)
            if ("keyagree".equals(id)) {
                m.put("publicKeyMultibase", ResponseMessage.Crypto.KA_PUBLIC_KEY);
                m.put("type", org.omnione.did.poc.pqc.server.config.PqcConfig.getKeyAgreementKeyType());
            } else {
                m.put("publicKeyMultibase", publicKey);
                m.put("type", keyType);
            }
            methods.add(m);
        }
        doc.put("verificationMethod", methods);
        doc.put("versionId", "1");
        return JsonUtils.GSON.toJson(doc);
    }
}

