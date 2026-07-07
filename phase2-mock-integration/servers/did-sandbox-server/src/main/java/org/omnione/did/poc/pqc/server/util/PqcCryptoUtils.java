package org.omnione.did.poc.pqc.server.util;

import org.omnione.did.poc.pqc.server.config.PqcConfig;
import org.omnione.did.crypto.enums.MultiBaseType;
import org.omnione.did.crypto.util.MultiBaseUtils;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * PQC 유틸리티 (ML-DSA-44 서명/검증 + ML-KEM-768 키교환).
 * 기존 ECC 로직에 영향 없이, PqcConfig 설정에 따라 사용된다.
 */
public class PqcCryptoUtils {

    private static final String ML_DSA_44 = "ML-DSA-44";
    private static final String ML_KEM_768 = "ML-KEM-768";
    private static final String PROVIDER = "BC";

    static {
        if (Security.getProvider(PROVIDER) == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
    }

    /**
     * ML-DSA-44 키 쌍 생성. MultiBase 인코딩된 공개키/개인키 반환.
     */
    public static String[] generateKeyPairEncoded() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ML_DSA_44, PROVIDER);
            KeyPair keyPair = kpg.generateKeyPair();

            String encodedPrivateKey = MultiBaseUtils.encode(keyPair.getPrivate().getEncoded(), MultiBaseType.base64);
            String encodedPublicKey = MultiBaseUtils.encode(keyPair.getPublic().getEncoded(), MultiBaseType.base64);

            return new String[]{encodedPrivateKey, encodedPublicKey};
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ML-DSA-44 key pair", e);
        }
    }

    /**
     * ML-DSA-44 서명 생성. 원본 데이터를 직접 서명 (해싱 불필요).
     * MultiBase base64 인코딩된 서명값 반환.
     */
    public static String sign(String privateKeyEncoded, byte[] data) {
        try {
            byte[] privateKeyBytes = MultiBaseUtils.decode(privateKeyEncoded);
            KeyFactory keyFactory = KeyFactory.getInstance(ML_DSA_44, PROVIDER);
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            Signature signer = Signature.getInstance(ML_DSA_44, PROVIDER);
            signer.initSign(privateKey);
            signer.update(data);
            byte[] signature = signer.sign();

            return MultiBaseUtils.encode(signature, MultiBaseType.base64);
        } catch (Exception e) {
            throw new RuntimeException("ML-DSA-44 signing failed", e);
        }
    }

    /**
     * ML-DSA-44 서명 검증.
     */
    public static void verify(String publicKeyEncoded, byte[] data, String signatureEncoded) {
        try {
            byte[] publicKeyBytes = MultiBaseUtils.decode(publicKeyEncoded);
            byte[] signatureBytes = MultiBaseUtils.decode(signatureEncoded);

            KeyFactory keyFactory = KeyFactory.getInstance(ML_DSA_44, PROVIDER);
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            Signature verifier = Signature.getInstance(ML_DSA_44, PROVIDER);
            verifier.initVerify(publicKey);
            verifier.update(data);

            if (!verifier.verify(signatureBytes)) {
                throw new RuntimeException("ML-DSA-44 signature verification failed");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("ML-DSA-44 verification failed", e);
        }
    }

    // ── ML-KEM-768 키교환 (BouncyCastle 네이티브 API) ──

    /**
     * [BC 호환성 우회]
     * BouncyCastle의 JCA Provider가 javax.crypto.KEM API를 지원하지 않으며,
     * JCA KeyFactory로 PKCS8 디코딩 후 생성된 PrivateKey로는 decapsulation이
     * 정상 동작하지 않는 문제가 있다 (decapKey length=84 등).
     *
     * 따라서 서버 시작 시 BC 저수준 API(MLKEMKeyPairGenerator)로 생성한
     * raw AsymmetricCipherKeyPair를 메모리에 보관하고,
     * decapsulate 시 이 raw key의 MLKEMPrivateKeyParameters를 직접 사용한다.
     *
     * 주의: 서버 재시작 시 키가 재생성되므로, 기존 세션의 ciphertext는 무효화됨.
     */
    private static org.bouncycastle.crypto.AsymmetricCipherKeyPair mlKemRawKeyPair;

    /**
     * ML-KEM-768 키 쌍 생성.
     * MultiBase 인코딩된 [decapsulationKey(PKCS8), encapsulationKey(X509)] 반환.
     * 동시에 raw key pair를 내부에 보관하여 decapsulate 시 사용.
     */
    public static String[] generateMlKemKeyPairEncoded() {
        try {
            // BouncyCastle 저수준 API로 키 생성 (raw params 보관 가능)
            org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator kemGen =
                    new org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator();
            kemGen.init(new org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters(
                    new java.security.SecureRandom(),
                    org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters.ml_kem_768));
            mlKemRawKeyPair = kemGen.generateKeyPair();

            // raw key pair의 인코딩을 직접 사용 (BC JCA provider의 ML-KEM-768 호환성 이슈 우회)
            org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters pubParams =
                    (org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters) mlKemRawKeyPair.getPublic();
            org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters privParams =
                    (org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters) mlKemRawKeyPair.getPrivate();

            // X509 인코딩 (encapsulation key)
            org.bouncycastle.asn1.x509.SubjectPublicKeyInfo pubInfo =
                    org.bouncycastle.pqc.crypto.util.SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(pubParams);
            byte[] encapKeyBytes = pubInfo.getEncoded();

            // PKCS8 인코딩 (decapsulation key)
            org.bouncycastle.asn1.pkcs.PrivateKeyInfo privInfo =
                    org.bouncycastle.pqc.crypto.util.PrivateKeyInfoFactory.createPrivateKeyInfo(privParams);
            byte[] decapKeyBytes = privInfo.getEncoded();

            String encodedDecapKey = MultiBaseUtils.encode(decapKeyBytes, MultiBaseType.base64);
            String encodedEncapKey = MultiBaseUtils.encode(encapKeyBytes, MultiBaseType.base64);

            return new String[]{encodedDecapKey, encodedEncapKey};
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ML-KEM-768 key pair", e);
        }
    }

    /**
     * ML-KEM-768 decapsulate. ciphertext로부터 shared secret(32바이트)을 추출한다.
     * BouncyCastle 저수준 API (MLKEMExtractor) 사용.
     *
     * @param decapKeyEncoded MultiBase 인코딩된 decapsulation key (현재는 raw key 사용, 미래 호환용)
     * @param ciphertextEncoded MultiBase 인코딩된 ciphertext (~1088바이트)
     * @return shared secret 바이트 배열 (32바이트)
     */
    public static byte[] mlKemDecapsulate(String decapKeyEncoded, String ciphertextEncoded) {
        try {
            byte[] ciphertextBytes = MultiBaseUtils.decode(ciphertextEncoded);

            if (mlKemRawKeyPair == null) {
                throw new IllegalStateException("ML-KEM key pair not initialized. Server restart required.");
            }

            org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters privParams =
                    (org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters) mlKemRawKeyPair.getPrivate();

            org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor extractor =
                    new org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor(privParams);

            byte[] sharedSecret = extractor.extractSecret(ciphertextBytes);
            return sharedSecret;
        } catch (Exception e) {
            throw new RuntimeException("ML-KEM-768 decapsulation failed", e);
        }
    }

    /**
     * Option 1 (Wallet-as-receiver) encapsulation result holder.
     * sharedSecret: 32바이트 대칭 키 재료 (세션 파생 입력).
     * ciphertextEncoded: MultiBase 인코딩된 ML-KEM ciphertext (~1088바이트), RES에 그대로 실어 보냄.
     */
    public static class MlKemEncapResult {
        public final byte[] sharedSecret;
        public final String ciphertextEncoded;
        MlKemEncapResult(byte[] sharedSecret, String ciphertextEncoded) {
            this.sharedSecret = sharedSecret;
            this.ciphertextEncoded = ciphertextEncoded;
        }
    }

    /**
     * ML-KEM-768 encapsulate (Option 1: TAS가 Wallet의 ephemeral pk에 대해 encap 수행).
     * 입력: MultiBase 인코딩된 X.509 SubjectPublicKeyInfo 형식의 클라이언트 공개키.
     * 출력: (sharedSecret 32B, ciphertext MultiBase base64).
     * 서버 장기 ML-KEM 키(mlKemRawKeyPair)에 의존하지 않는다.
     */
    public static MlKemEncapResult mlKemEncapsulate(String clientPublicKeyEncoded) {
        try {
            byte[] pkBytes = MultiBaseUtils.decode(clientPublicKeyEncoded);
            org.bouncycastle.crypto.params.AsymmetricKeyParameter pubParam =
                    org.bouncycastle.pqc.crypto.util.PublicKeyFactory.createKey(pkBytes);
            if (!(pubParam instanceof org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters)) {
                throw new IllegalArgumentException(
                        "Not an ML-KEM public key: " + pubParam.getClass().getName());
            }
            org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters pk =
                    (org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters) pubParam;

            org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator generator =
                    new org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator(new java.security.SecureRandom());
            org.bouncycastle.crypto.SecretWithEncapsulation result =
                    generator.generateEncapsulated(pk);

            byte[] sharedSecret = result.getSecret();
            byte[] ciphertext = result.getEncapsulation();
            String ctEncoded = MultiBaseUtils.encode(ciphertext, MultiBaseType.base64);
            return new MlKemEncapResult(sharedSecret, ctEncoded);
        } catch (Exception e) {
            throw new RuntimeException("ML-KEM-768 encapsulation failed", e);
        }
    }
}
