package org.omnione.did.poc.pqc.server.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * PQC 알고리즘 설정.
 * application.yml의 pqc.signature-algorithm / pqc.key-agreement-algorithm 값에 따라
 * 서버 전체의 서명/키교환 타입이 결정된다.
 *
 * 서명 지원값:
 *   - Secp256r1 (기본��, 기존 ECC)
 *   - MlDsa44   (PQC ML-DSA-44)
 *
 * 키교환 지원값:
 *   - Secp256r1 (기본���, 기존 ECDH)
 *   - MlKem768  (PQC ML-KEM-768)
 */
@Configuration
public class PqcConfig {

    private static String signatureAlgorithm = "Secp256r1";
    private static String keyAgreementAlgorithm = "Secp256r1";

    @Value(value = "${pqc.signature-algorithm:Secp256r1}")
    private String signatureAlgorithmProperty;

    @Value(value = "${pqc.key-agreement-algorithm:Secp256r1}")
    private String keyAgreementAlgorithmProperty;

    @PostConstruct
    public void init() {
        signatureAlgorithm = this.signatureAlgorithmProperty;
        keyAgreementAlgorithm = this.keyAgreementAlgorithmProperty;
    }

    public static boolean isMlDsa44() {
        return "MlDsa44".equalsIgnoreCase(signatureAlgorithm);
    }

    public static boolean isMlKem768() {
        return "MlKem768".equalsIgnoreCase(keyAgreementAlgorithm);
    }

    public static String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public static String getKeyAgreementAlgorithm() {
        return keyAgreementAlgorithm;
    }

    /**
     * 현재 설정에 맞는 proof type 문자열 반환.
     */
    public static String getProofType() {
        return isMlDsa44() ? "MlDsa44Signature2024" : "Secp256r1Signature2018";
    }

    /**
     * 현재 설정에 맞는 DID verification key type 문자열 반환.
     */
    public static String getVerificationKeyType() {
        return isMlDsa44() ? "MlDsa44VerificationKey2024" : "Secp256r1VerificationKey2018";
    }

    /**
     * keyAgreement proof type 반환. ML-KEM은 서명 불가이므로 ML-DSA-44로 서명.
     */
    public static String getKeyAgreementProofType() {
        return isMlKem768() ? "MlDsa44Signature2024" : "Secp256r1Signature2018";
    }

    /**
     * keyAgreement DID verification method type 반환.
     */
    public static String getKeyAgreementKeyType() {
        return isMlKem768() ? "MlKem768AgreementKey2024" : "Secp256r1VerificationKey2018";
    }
}
