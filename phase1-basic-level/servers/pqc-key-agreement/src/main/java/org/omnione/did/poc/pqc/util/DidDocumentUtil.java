package org.omnione.did.poc.pqc.util;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.omnione.did.crypto.enums.EccCurveType;
import org.omnione.did.crypto.util.CryptoUtils;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.data.model.did.DidDocument;
import org.omnione.did.data.model.did.VerificationMethod;

// DID Document 유틸
public class DidDocumentUtil {

    public static final String KEY_TYPE_MLDSA44 = "MlDsa44VerificationKey2024";
    public static final String KEY_TYPE_MLKEM768 = "MlKem768KeyAgreementKey2024";
    public static final String KEY_TYPE_SECP256R1 = "Secp256r1VerificationKey2018";

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    // DidDocument 생성
    public static DidDocument createDidDocument(String did, String controller) {
        DidDocument doc = new DidDocument();
        doc.setContext(List.of("https://www.w3.org/ns/did/v1"));
        doc.setId(did);
        doc.setController(controller);
        doc.setCreated(Instant.now().toString());
        doc.setUpdated(doc.getCreated());
        doc.setDeactivated(false);
        doc.setVersionId("1");
        doc.setVerificationMethod(new ArrayList<>());
        doc.setAssertionMethod(new ArrayList<>());
        doc.setAuthentication(new ArrayList<>());
        doc.setKeyAgreement(new ArrayList<>());
        return doc;
    }

    // verificationMethod 추가 (multibase = "m" + Base64) - ML-DSA용
    public static void addVerificationMethod(DidDocument doc, String keyId,
                                             PublicKey publicKey, String... purposes) {
        addVerificationMethod(doc, keyId, KEY_TYPE_MLDSA44, publicKey, purposes);
    }

    // verificationMethod 추가 - 키 타입 지정 가능
    public static void addVerificationMethod(DidDocument doc, String keyId,
                                             String keyType, PublicKey publicKey,
                                             String... purposes) {
        String multibase = "m" + Base64.getEncoder().encodeToString(publicKey.getEncoded());
        addVerificationMethodWithMultibase(doc, keyId, keyType, multibase, purposes);
    }

    // verificationMethod 추가 - multibase 문자열 직접 지정 (Wallet SDK 압축 공개키용)
    public static void addVerificationMethodWithMultibase(DidDocument doc, String keyId,
                                                          String keyType, String publicKeyMultibase,
                                                          String... purposes) {
        VerificationMethod vm = new VerificationMethod();
        vm.setId(keyId);
        vm.setType(keyType);
        vm.setController(doc.getController());
        vm.setPublicKeyMultibase(publicKeyMultibase);
        vm.setAuthType(1);

        doc.getVerificationMethod().add(vm);

        for (String purpose : purposes) {
            switch (purpose) {
                case "assertionMethod" -> doc.getAssertionMethod().add(keyId);
                case "authentication" -> doc.getAuthentication().add(keyId);
                case "keyAgreement" -> doc.getKeyAgreement().add(keyId);
            }
        }
    }

    // keyId로 verificationMethod 조회
    public static VerificationMethod getVerificationMethodById(DidDocument doc, String keyId) {
        return doc.getVerificationMethod().stream()
                .filter(vm -> vm.getId().equals(keyId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "VerificationMethod not found: " + keyId));
    }

    // 공개키 복원 (multibase 'm' base64 + 'z' base58btc 지원)
    public static PublicKey getPublicKey(DidDocument doc, String keyId) throws Exception {
        VerificationMethod vm = getVerificationMethodById(doc, keyId);
        String multibase = vm.getPublicKeyMultibase();

        byte[] decoded = MultiBaseUtils.decode(multibase);

        String algorithm = switch (vm.getType()) {
            case KEY_TYPE_MLKEM768 -> "ML-KEM-768";
            case KEY_TYPE_MLDSA44 -> "ML-DSA-44";
            case KEY_TYPE_SECP256R1 -> "EC";
            default -> throw new IllegalArgumentException("Unsupported key type: " + vm.getType());
        };

        // EC 압축 공개키(33 bytes)인 경우 비압축 후 X509로 변환
        if ("EC".equals(algorithm) && decoded.length == 33) {
            decoded = CryptoUtils.unCompressPublicKey(decoded, EccCurveType.Secp256r1);
        }

        KeyFactory kf = KeyFactory.getInstance(algorithm, "BC");
        return kf.generatePublic(new X509EncodedKeySpec(decoded));
    }
}
