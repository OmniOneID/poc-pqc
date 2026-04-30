package org.omnione.did.poc.pqc.server.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.omnione.did.crypto.enums.DigestType;
import org.omnione.did.crypto.enums.EccCurveType;
import org.omnione.did.crypto.util.DigestUtils;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.crypto.util.SignatureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * VP/VC 서명 검증 유틸리티.
 * DID document에서 공개키를 조회하고, proof type에 따라 ECC 또는 ML-DSA-44 서명을 검증한다.
 */
public class ProofVerifier {

    private static final Logger log = LoggerFactory.getLogger(ProofVerifier.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * VP의 outer proof 검증.
     * @param vpJson 복호화된 VP JSON
     * @return 검증 결과 (성공 시 null, 실패 시 에러 메시지)
     */
    public static String verifyVpProof(String vpJson) {
        try {
            Map<String, Object> vp = objectMapper.readValue(vpJson, new TypeReference<>() {});
            Map<String, Object> proof = (Map<String, Object>) vp.get("proof");
            if (proof == null) return "VP proof is missing";

            String proofType = (String) proof.get("type");
            String proofValue = (String) proof.get("proofValue");
            String verificationMethod = (String) proof.get("verificationMethod");

            if (proofValue == null || proofValue.isEmpty()) return "VP proofValue is empty";

            // verificationMethod에서 DID와 keyId 추출
            // 형식: did:omn:xxx?versionId=1#keyId
            String did = verificationMethod.split("\\?")[0];
            String keyId = verificationMethod.contains("#") ? verificationMethod.split("#")[1] : null;

            // DID document에서 공개키 조회
            String publicKey = lookupPublicKey(did, keyId);
            if (publicKey == null) return "Public key not found for " + verificationMethod;

            // 서명 대상 데이터 재구성: proofValue를 제거하고 직렬화
            Map<String, Object> vpForVerify = new LinkedHashMap<>(vp);
            Map<String, Object> proofForVerify = new LinkedHashMap<>(proof);
            proofForVerify.remove("proofValue");
            vpForVerify.put("proof", proofForVerify);
            String signedData = org.omnione.did.common.util.JsonUtil.serializeAndSort(vpForVerify);

            // proof type에 따라 검증
            return verifySignature(proofType, publicKey, signedData, proofValue);

        } catch (Exception e) {
            log.error("VP proof verification error", e);
            return "VP verification error: " + e.getMessage();
        }
    }

    /**
     * VC의 proofValue 검증 (전체 VC 서명).
     * @param vcMap VC object (Map)
     * @return 검증 결과 (성공 시 null, 실패 시 에러 메시지)
     */
    public static String verifyVcProof(Map<String, Object> vcMap) {
        try {
            Map<String, Object> proof = (Map<String, Object>) vcMap.get("proof");
            if (proof == null) return "VC proof is missing";

            String proofType = (String) proof.get("type");
            String proofValue = (String) proof.get("proofValue");
            String verificationMethod = (String) proof.get("verificationMethod");

            // VP에서 제출된 VC는 선택적 공개로 proofValue가 비어있을 수 있음 — 스킵
            if (proofValue == null || proofValue.isEmpty()) return null;

            String did = verificationMethod.split("\\?")[0];
            String keyId = verificationMethod.contains("#") ? verificationMethod.split("#")[1] : null;

            String publicKey = lookupPublicKey(did, keyId);
            if (publicKey == null) return "Public key not found for " + verificationMethod;

            // 서명 대상: proofValue와 proofValueList 제거
            Map<String, Object> vcForVerify = new LinkedHashMap<>(vcMap);
            Map<String, Object> proofForVerify = new LinkedHashMap<>(proof);
            proofForVerify.remove("proofValue");
            proofForVerify.remove("proofValueList");
            vcForVerify.put("proof", proofForVerify);
            String signedData = org.omnione.did.common.util.JsonUtil.serializeAndSort(vcForVerify);

            return verifySignature(proofType, publicKey, signedData, proofValue);

        } catch (Exception e) {
            log.error("VC proof verification error", e);
            return "VC verification error: " + e.getMessage();
        }
    }

    /**
     * VC의 proofValueList 검증 (claim별 개별 서명).
     * @param vcMap VC object (Map)
     * @return 검증 결과 목록 (각 claim별, 성공 시 null, 실패 시 에러 메시지)
     */
    public static List<String> verifyVcProofValueList(Map<String, Object> vcMap) {
        List<String> results = new ArrayList<>();
        try {
            Map<String, Object> proof = (Map<String, Object>) vcMap.get("proof");
            if (proof == null) { results.add("VC proof is missing"); return results; }

            List<String> proofValueList = (List<String>) proof.get("proofValueList");
            if (proofValueList == null || proofValueList.isEmpty()) return results; // proofValueList 없으면 스킵

            String proofType = (String) proof.get("type");
            String verificationMethod = (String) proof.get("verificationMethod");
            String did = verificationMethod.split("\\?")[0];
            String keyId = verificationMethod.contains("#") ? verificationMethod.split("#")[1] : null;

            String publicKey = lookupPublicKey(did, keyId);
            if (publicKey == null) { results.add("Public key not found"); return results; }

            // 검증 SDK와 동일한 방식: 전체 VC를 복사하고 claims를 단일 claim으로 교체
            Map<String, Object> credentialSubject = (Map<String, Object>) vcMap.get("credentialSubject");
            List<Map<String, Object>> claims = (List<Map<String, Object>>) credentialSubject.get("claims");

            for (int i = 0; i < claims.size() && i < proofValueList.size(); i++) {
                // 단일 claim VC 재구성
                Map<String, Object> singleClaimVc = new LinkedHashMap<>(vcMap);
                Map<String, Object> singleSubject = new LinkedHashMap<>(credentialSubject);
                singleSubject.put("claims", Collections.singletonList(claims.get(i)));
                singleClaimVc.put("credentialSubject", singleSubject);

                // proof에서 proofValue, proofValueList 제거
                Map<String, Object> cleanProof = new LinkedHashMap<>(proof);
                cleanProof.remove("proofValue");
                cleanProof.remove("proofValueList");
                singleClaimVc.put("proof", cleanProof);

                String signedData = org.omnione.did.common.util.JsonUtil.serializeAndSort(singleClaimVc);
                String result = verifySignature(proofType, publicKey, signedData, proofValueList.get(i));
                results.add(result); // null = success
            }
        } catch (Exception e) {
            log.error("VC proofValueList verification error", e);
            results.add("proofValueList verification error: " + e.getMessage());
        }
        return results;
    }

    /**
     * proof type에 따라 서명 검증 수행.
     * @return 성공 시 null, 실패 시 에러 메시지
     */
    private static String verifySignature(String proofType, String publicKeyEncoded, String data, String signatureEncoded) {
        try {
            if (isMlDsa44(proofType)) {
                // ML-DSA-44: 원본 데이터로 직접 검증
                PqcCryptoUtils.verify(publicKeyEncoded, data.getBytes(StandardCharsets.UTF_8), signatureEncoded);
            } else {
                // ECC: SHA-256 해시 후 compact signature 검증
                byte[] hashedData = DigestUtils.getDigest(data.getBytes(StandardCharsets.UTF_8), DigestType.SHA256);
                byte[] signatureBytes = MultiBaseUtils.decode(signatureEncoded);
                byte[] publicKeyBytes = MultiBaseUtils.decode(publicKeyEncoded);
                SignatureUtils.verifyCompactSignWithCompressedKey(publicKeyBytes, hashedData, signatureBytes, EccCurveType.Secp256r1);
            }
            return null; // 성공
        } catch (Exception e) {
            return "Signature invalid: " + e.getMessage();
        }
    }

    private static boolean isMlDsa44(String proofType) {
        return proofType != null && proofType.toLowerCase().contains("mldsa");
    }

    /**
     * user_did 파일에서 DID document를 조회하고, 지정된 keyId의 공개키를 반환.
     */
    private static String lookupPublicKey(String did, String keyId) {
        String[] result = lookupPublicKeyWithType(did, keyId);
        return result != null ? result[0] : null;
    }

    /**
     * user_did 파일에서 DID document를 조회하고, 지정된 keyId의 [공개키, type]을 반환.
     * type: "Secp256r1VerificationKey2018", "MlDsa44VerificationKey2024", "MlKem768AgreementKey2024" 등
     */
    public static String[] lookupPublicKeyWithType(String did, String keyId) {
        try {
            File file = new File(MockDataInitializer.getUserDidDataPath());
            if (!file.exists()) return null;

            List<Map<String, Object>> users = objectMapper.readValue(file, new TypeReference<>() {});
            for (Map<String, Object> user : users) {
                if (!did.equals(user.get("holderDID"))) continue;

                String encodedDidDoc = (String) user.get("ownerDidDoc");
                byte[] decoded = MultiBaseUtils.decode(encodedDidDoc);
                String didDocJson = new String(decoded, StandardCharsets.UTF_8);
                Map<String, Object> didDoc = objectMapper.readValue(didDocJson, new TypeReference<>() {});

                List<Map<String, Object>> verificationMethods =
                        (List<Map<String, Object>>) didDoc.get("verificationMethod");
                if (verificationMethods == null) return null;

                for (Map<String, Object> method : verificationMethods) {
                    if (keyId == null || keyId.equals(method.get("id"))) {
                        String publicKey = (String) method.get("publicKeyMultibase");
                        String type = (String) method.get("type");
                        return new String[]{publicKey, type};
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to lookup public key for {}#{}", did, keyId, e);
        }
        return null;
    }

    /**
     * verificationMethod type이 ML-KEM-768 키 교환 키인지 판별.
     */
    public static boolean isMlKem768KeyType(String type) {
        return type != null && type.contains("MlKem768");
    }

    /**
     * verificationMethod type이 EC(Secp256r1) 키인지 판별.
     */
    public static boolean isEcKeyType(String type) {
        return type != null && type.contains("Secp256r1");
    }
}
