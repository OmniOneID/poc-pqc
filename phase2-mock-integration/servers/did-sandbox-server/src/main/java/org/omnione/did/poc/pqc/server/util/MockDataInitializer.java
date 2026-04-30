package org.omnione.did.poc.pqc.server.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.omnione.did.poc.pqc.server.ResponseMessage;
import org.omnione.did.poc.pqc.server.config.PqcConfig;
import org.omnione.did.crypto.enums.MultiBaseType;
import org.omnione.did.crypto.util.MultiBaseUtils;

import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ML-DSA-44 모드일 때 DID_DOC, CERT_VC 등 정적 모킹 데이터를 동적으로 재생성한다.
 * 기존 ECC 모드에서는 호출되지 않으므로 기존 로직에 영향 없음.
 */
public class MockDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(MockDataInitializer.class);
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private static final String USER_DID_DATA_PATH_ECC = "src/main/resources/user_did_secp256r1.json";
    private static final String USER_DID_DATA_PATH_MLDSA = "src/main/resources/user_did_mldsa44.json";

    /**
     * 현재 알고리즘 설정에 맞는 user_did 파일 경로 반환.
     * Secp256r1: user_did_secp256r1.json
     * MlDsa44:   user_did_mldsa44.json
     */
    public static String getUserDidDataPath() {
        return PqcConfig.isMlDsa44() ? USER_DID_DATA_PATH_MLDSA : USER_DID_DATA_PATH_ECC;
    }

    /** mock 서버 운영에 필수인 서버 DID 목록 */
    private static final List<String> SERVER_DIDS = List.of(
            "did:omn:tas", "did:omn:cas", "did:omn:issuer", "did:omn:wallet", "did:omn:verifier"
    );

    /**
     * 서버 기동 시 현재 알고리즘 모드에 맞는 초기 데이터를 세팅한다.
     * 반드시 Crypto.initKeys() 이후에 호출되어야 한다.
     *
     * - 양쪽 모드 모두: user_did 파일에 서버 DID 항목 추가/갱신
     * - ML-DSA-44 모드: ResponseMessage.Data의 정적 데이터도 재생성
     */
    public static void initMockData() {
        // 양쪽 모드 모두: 서버 DID 항목 초기화
        initUserDidJson();
    }

    /**
     * 현재 알고리즘에 맞는 user_did 파일에 서버 DID 항목들을 추가/갱신한다.
     * 이미 해당 DID가 존재하면 덮어쓰고, 없으면 추가한다.
     * 기존 사용자(홀더) DID 항목은 그대로 유지된다.
     */
    private static void initUserDidJson() {
        try {
            File file = new File(getUserDidDataPath());
            List<Map<String, Object>> users;

            if (file.exists() && file.length() > 2) {
                try (FileReader reader = new FileReader(file)) {
                    users = gson.fromJson(reader, new TypeToken<List<Map<String, Object>>>(){}.getType());
                }
                if (users == null) users = new ArrayList<>();
            } else {
                users = new ArrayList<>();
            }

            String publicKey = ResponseMessage.Crypto.PUBLIC_KEY;
            String keyType = PqcConfig.getVerificationKeyType();

            for (String did : SERVER_DIDS) {
                String ownerDidDoc = encodeDidDocument(did, publicKey, keyType);

                boolean found = false;
                for (Map<String, Object> user : users) {
                    if (did.equals(user.get("holderDID"))) {
                        user.put("ownerDidDoc", ownerDidDoc);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("holderDID", did);
                    entry.put("ownerDidDoc", ownerDidDoc);
                    users.add(entry);
                }
            }

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(users, writer);
            }
            log.info("{} initialized with {} server DIDs: {}", getUserDidDataPath(), PqcConfig.getSignatureAlgorithm(), SERVER_DIDS);

        } catch (Exception e) {
            log.error("Failed to initialize {}", getUserDidDataPath(), e);
        }
    }

    /**
     * 주어진 DID에 대한 DID document를 생성하고 MultiBase 인코딩하여 반환.
     */
    private static String encodeDidDocument(String did, String publicKey, String keyType) {
        Map<String, Object> didDoc = new LinkedHashMap<>();
        didDoc.put("@context", List.of("https://www.w3.org/ns/did/v1"));
        didDoc.put("assertionMethod", List.of("assert"));
        didDoc.put("authentication", List.of("auth"));
        didDoc.put("capabilityDelegation", List.of());
        didDoc.put("capabilityInvocation", List.of("invoke"));
        didDoc.put("controller", "did:omn:tas");
        didDoc.put("created", "2025-06-11T04:32:44Z");
        didDoc.put("deactivated", false);
        didDoc.put("id", did);
        didDoc.put("keyAgreement", List.of("keyagree"));
        didDoc.put("updated", "2025-06-11T04:32:44Z");

        List<Map<String, Object>> verificationMethods = new ArrayList<>();
        for (String id : List.of("assert", "auth", "keyagree", "invoke")) {
            Map<String, Object> method = new LinkedHashMap<>();
            method.put("authType", 1);
            method.put("controller", did);
            method.put("id", id);
            // keyagree: 설정에 따라 ML-KEM-768 또는 ECDH(Secp256r1)
            if ("keyagree".equals(id)) {
                method.put("publicKeyMultibase", org.omnione.did.poc.pqc.server.ResponseMessage.Crypto.KA_PUBLIC_KEY);
                method.put("type", PqcConfig.getKeyAgreementKeyType());
            } else {
                method.put("publicKeyMultibase", publicKey);
                method.put("type", keyType);
            }
            verificationMethods.add(method);
        }
        didDoc.put("verificationMethod", verificationMethods);
        didDoc.put("versionId", "1");

        try {
            String json = gson.toJson(didDoc);
            return MultiBaseUtils.encode(json.getBytes(StandardCharsets.UTF_8), MultiBaseType.base64);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode DID doc for " + did, e);
        }
    }

}
