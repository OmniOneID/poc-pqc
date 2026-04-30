package org.omnione.did.poc.pqc.server.util;

import org.omnione.did.poc.pqc.server.ResponseMessage;
import org.omnione.did.data.model.did.Proof;
import org.omnione.did.data.model.provider.Provider;
import org.omnione.did.data.model.provider.ProviderDetail;
import org.omnione.did.data.model.vc.VerifiableCredential;
import org.omnione.did.data.model.vc.Claim;
import org.omnione.did.data.model.vc.CredentialSchema;
import org.omnione.did.data.model.vc.CredentialSubject;
import org.omnione.did.data.model.vc.DocumentVerificationEvidence;
import org.omnione.did.data.model.vc.Issuer;
import org.omnione.did.data.model.vc.VcMeta;
import org.omnione.did.data.model.vc.VcProof;
import org.omnione.did.data.model.schema.VcSchema;
import org.omnione.did.data.model.schema.ClaimDef;
import org.omnione.did.data.model.schema.MetaData;
import org.omnione.did.data.model.schema.Namespace;
import org.omnione.did.data.model.schema.SchemaClaims;
import org.omnione.did.data.model.schema.SchemaCredentialSubject;
import org.omnione.did.data.model.profile.Filter;
import org.omnione.did.data.model.profile.ReqE2e;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockDataFactory {

    /** 현재 UTC 시간을 ISO format으로 반환 */
    public static String now() {
        return java.time.Instant.now().atOffset(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ISO_INSTANT);
    }

    /** 현재 시간 + 1년을 ISO format으로 반환 (validUntil 용) */
    public static String nowPlusOneYear() {
        return java.time.Instant.now().atOffset(java.time.ZoneOffset.UTC)
                .plusYears(1)
                .format(java.time.format.DateTimeFormatter.ISO_INSTANT);
    }

    /** 16바이트 nonce를 생성하여 MultiBase base64로 반환 */
    public static String generateNonce() {
        try {
            return org.omnione.did.crypto.util.MultiBaseUtils.encode(
                    org.omnione.did.crypto.util.CryptoUtils.generateNonce(16),
                    org.omnione.did.crypto.enums.MultiBaseType.base64);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate nonce", e);
        }
    }

    // ── Proof ──

    public static Proof createProof(String type, String created, String verificationMethod, String proofPurpose) {
        Proof proof = new Proof();
        proof.setType(type);
        proof.setCreated(created);
        proof.setVerificationMethod(verificationMethod);
        proof.setProofPurpose(proofPurpose);
        return proof;
    }

    /**
     * 설정에 따라 적절한 proof type으로 Proof 생성.
     */
    public static Proof createSignatureProof(String created, String verificationMethod) {
        return createProof(
                org.omnione.did.poc.pqc.server.config.PqcConfig.getProofType(),
                created, verificationMethod, "assertionMethod");
    }

    // ── Provider ──

    public static Provider createProvider(String did, String certVcRef) {
        Provider provider = new Provider();
        provider.setDid(did);
        provider.setCertVcRef(certVcRef);
        return provider;
    }

    public static ProviderDetail createProviderDetail(String did, String certVcRef, String name, String ref) {
        ProviderDetail detail = new ProviderDetail();
        detail.setDid(did);
        detail.setCertVcRef(certVcRef);
        detail.setName(name);
        detail.setRef(ref);
        return detail;
    }

    // ── Claim ──

    public static Claim createClaim(String code, String caption, String type, String format, boolean hideValue, String value) {
        Claim claim = new Claim();
        claim.setCode(code);
        claim.setCaption(caption);
        claim.setType(type);
        claim.setFormat(format);
        claim.setHideValue(hideValue);
        claim.setValue(value);
        return claim;
    }

    // ── CredentialSchema ──

    public static CredentialSchema createCredentialSchema(String id, String type) {
        CredentialSchema schema = new CredentialSchema();
        schema.setId(id);
        schema.setType(type);
        return schema;
    }

    public static CredentialSchema createStudentIdSchema() {
        return createCredentialSchema(
                ResponseMessage.Common.SERVER_URL + "/issuer/api/v1/vc/vcschema?name=omnione.student_id",
                "OsdSchemaCredential"
        );
    }

    // ── Issuer ──

    public static Issuer createIssuer(String id, String name) {
        Issuer issuer = new Issuer();
        issuer.setId(id);
        issuer.setName(name);
        return issuer;
    }

    // ── CredentialSubject ──

    public static CredentialSubject createCredentialSubject(String subjectId, List<Claim> claims) {
        CredentialSubject cs = new CredentialSubject();
        cs.setId(subjectId);
        cs.setClaims(claims);
        return cs;
    }

    // ── Evidence ──

    public static DocumentVerificationEvidence createDocVerificationEvidence(String verifier) {
        DocumentVerificationEvidence evidence = new DocumentVerificationEvidence();
        evidence.setType("DocumentVerification");
        evidence.setVerifier(verifier);
        evidence.setDocumentPresence("Physical");
        evidence.setEvidenceDocument("BusinessLicense");
        evidence.setSubjectPresence("Physical");
        return evidence;
    }

    public static DocumentVerificationEvidence createDocVerificationEvidenceWithAttribute(
            String verifier, Map<String, String> attribute) {
        DocumentVerificationEvidence evidence = createDocVerificationEvidence(verifier);
        evidence.setAttribute(attribute);
        return evidence;
    }

    // ── CertificateVC (공통 패턴) ──

    public static VerifiableCredential createCertificateVc(
            String subjectDid, String subjectValue, String roleValue,
            String issuerDid, String issuerName, String proofVerificationMethod) {

        Claim subjectClaim = createClaim("org.opendid.v1.subject", "subject", "text", "plain", false, subjectValue);
        Claim roleClaim = createClaim("org.opendid.v1.role", "role", "text", "plain", false, roleValue);

        CredentialSubject credentialSubject = createCredentialSubject(subjectDid, Arrays.asList(subjectClaim, roleClaim));

        CredentialSchema credentialSchema = createCredentialSchema(
                ResponseMessage.Common.SERVER_URL + "/tas/api/v1/vc-schema?name=certificate",
                "OsdSchemaCredential"
        );

        DocumentVerificationEvidence evidence = createDocVerificationEvidence("did:omn:tas");

        Issuer issuer = createIssuer(issuerDid, issuerName);

        VcProof proof = new VcProof();
        proof.setType(org.omnione.did.poc.pqc.server.config.PqcConfig.getProofType());
        proof.setCreated(now());
        proof.setVerificationMethod(proofVerificationMethod);
        proof.setProofPurpose("assertionMethod");

        VerifiableCredential vc = new VerifiableCredential();
        vc.setContext();
        vc.setId("952a639e-1f99-42a8-bd13-788e5d055051");
        vc.setType(Arrays.asList("VerifiableCredential", "CertificateVC"));
        vc.setCredentialSchema(credentialSchema);
        vc.setCredentialSubject(credentialSubject);
        vc.setEvidence(Collections.singletonList(evidence));
        vc.setIssuer(issuer);
        vc.setIssuanceDate(now());
        vc.setValidFrom(now());
        vc.setValidUntil(nowPlusOneYear());
        vc.setEncoding("UTF-8");
        vc.setFormatVersion("1.0");
        vc.setLanguage("ko");
        vc.setProof(proof);

        return vc;
    }

    public static VerifiableCredential createCertificateVcWithAttribute(
            String subjectDid, String subjectValue, String roleValue,
            String issuerDid, String issuerName, String proofVerificationMethod,
            Map<String, String> attribute) {

        VerifiableCredential vc = createCertificateVc(
                subjectDid, subjectValue, roleValue, issuerDid, issuerName, proofVerificationMethod);

        DocumentVerificationEvidence evidence = createDocVerificationEvidenceWithAttribute("did:omn:tas", attribute);
        vc.setEvidence(Collections.singletonList(evidence));

        return vc;
    }

    // ── VcMeta ──

    public static VcMeta createVcMeta(String vcId, String issuerDid, String issuerCertVcRef,
                                       String subject, String status) {
        VcMeta meta = new VcMeta();
        meta.setId(vcId);

        Provider issuerProvider = createProvider(issuerDid, issuerCertVcRef);
        meta.setIssuer(issuerProvider);

        meta.setSubject(subject);
        meta.setCredentialSchema(createStudentIdSchema());
        meta.setStatus(status);
        meta.setIssuanceDate(now());
        meta.setValidFrom(now());
        meta.setValidUntil(nowPlusOneYear());
        meta.setFormatVersion("1.0");
        meta.setLanguage("ko");

        return meta;
    }

    // ── VcSchema ──

    public static VcSchema createStudentIdVcSchema() {
        VcSchema schema = new VcSchema();
        schema.setId(ResponseMessage.Common.SERVER_URL + "/issuer/api/v1/vc/vcschema?name=omnione.student_id");
        schema.setSchema("https://opendid.org/schema/vc.osd");
        schema.setTitle("Student ID");
        schema.setDescription("Student ID");

        MetaData metadata = new MetaData();
        metadata.setFormatVersion("1.0");
        metadata.setLanguage("en");
        schema.setMetadata(metadata);

        Namespace namespace = new Namespace();
        namespace.setId("org.omnione.student_id");
        namespace.setName("Student ID");
        namespace.setRef("https://omnione.org");

        List<ClaimDef> items = Arrays.asList(
                createClaimDef("name", "Name", "text", "plain", false),
                createClaimDef("student_id", "Student ID", "text", "plain", false),
                createClaimDef("emision_date", "Emision Date", "text", "plain", false),
                createClaimDef("major", "Major", "text", "plain", false),
                createClaimDef("expiration_date", "Expiration Date", "text", "plain", false)
        );

        SchemaClaims schemaClaims = new SchemaClaims();
        schemaClaims.setNamespace(namespace);
        schemaClaims.setItems(items);

        SchemaCredentialSubject credentialSubject = new SchemaCredentialSubject();
        credentialSubject.setClaims(Collections.singletonList(schemaClaims));
        schema.setCredentialSubject(credentialSubject);

        return schema;
    }

    public static ClaimDef createClaimDef(String id, String caption, String type, String format, boolean hideValue) {
        ClaimDef def = new ClaimDef();
        def.setId(id);
        def.setCaption(caption);
        def.setType(type);
        def.setFormat(format);
        def.setHideValue(hideValue);
        return def;
    }

    // ── ReqE2e ──

    public static ReqE2e createReqE2e(String nonce, String curve, String publicKey, String cipher, String padding) {
        ReqE2e reqE2e = new ReqE2e();
        reqE2e.setNonce(nonce);
        reqE2e.setCurve(curve);
        reqE2e.setPublicKey(publicKey);
        reqE2e.setCipher(cipher);
        reqE2e.setPadding(padding);
        return reqE2e;
    }

    /**
     * ML-KEM-768 모드용 ReqE2e를 Map으로 생성.
     *
     * [SDK 제약 우회]
     * OpenDID SDK의 ReqE2e 모델에는 "algorithm" 필드가 정의되어 있지 않아
     * ML-KEM-768을 타입 안전하게 표현할 수 없다.
     * 따라서 Map으로 직접 구성하여 JSON 직렬화 시 algorithm 필드가 포함되도록 한다.
     * SDK가 ML-KEM을 지원하게 되면 ReqE2e 모델로 전환할 것.
     *
     * proof는 ML-DSA-44로 서명 (ML-KEM 키는 서명 불가).
     */
    public static Map<String, Object> createMlKemReqE2eMap() {
        Map<String, Object> reqE2e = new java.util.LinkedHashMap<>();
        reqE2e.put("algorithm", "MlKem768");
        reqE2e.put("publicKey", ResponseMessage.Crypto.MLKEM_ENCAP_KEY);
        reqE2e.put("nonce", ResponseMessage.Crypto.E2E_NONCE_VC);
        reqE2e.put("cipher", "AES-256-CBC");
        reqE2e.put("padding", "PKCS5");
        // proof는 호출부에서 추가
        return reqE2e;
    }

    /**
     * 현재 설정에 따라 적절한 ReqE2e 반환. ML-KEM이면 Map, ECDH이면 ReqE2e 객체.
     * JSON 직렬화 시 모두 동일하게 동작한다.
     */
    public static Object createDefaultReqE2eAuto() {
        if (org.omnione.did.poc.pqc.server.config.PqcConfig.isMlKem768()) {
            return createMlKemReqE2eMap();
        } else {
            return createDefaultReqE2e();
        }
    }

    public static ReqE2e createDefaultReqE2e() {
        // ECDH는 항상 Secp256r1 + ECC 공개키 사용
        return createReqE2e(
                ResponseMessage.Crypto.E2E_NONCE_VC,
                "Secp256r1",
                ResponseMessage.Crypto.ECC_PUBLIC_KEY,
                "AES-256-CBC",
                "PKCS5"
        );
    }

    // ── Student ID VC (발급용) ──

    public static VerifiableCredential createStudentIdVc(String vcId, String holderDid) {
        List<Claim> claims = Arrays.asList(
                createClaim("org.omnione.student_id.name", "Name", "text", "plain", false, "Raon Kim"),
                createClaim("org.omnione.student_id.student_id", "Student ID", "text", "plain", false, "2025111001"),
                createClaim("org.omnione.student_id.major", "Major", "text", "plain", false, "Computer Science"),
                createClaim("org.omnione.student_id.emision_date", "Emision Date", "text", "plain", false, "2026-01-01T00:00:00.000000000Z"),
                createClaim("org.omnione.student_id.expiration_date", "Expiration Date", "text", "plain", false, "2027-01-01T00:00:00.000000000Z")
        );

        CredentialSubject credentialSubject = createCredentialSubject(holderDid, claims);
        CredentialSchema credentialSchema = createStudentIdSchema();
        DocumentVerificationEvidence evidence = createDocVerificationEvidence("did:omn:tas");
        Issuer issuer = createIssuer("did:omn:issuer", "issuer");

        VcProof proof = new VcProof();
        proof.setType(org.omnione.did.poc.pqc.server.config.PqcConfig.getProofType());
        proof.setCreated(now());
        proof.setVerificationMethod("did:omn:issuer?versionId=1#assert");
        proof.setProofPurpose("assertionMethod");

        VerifiableCredential vc = new VerifiableCredential();
        vc.setContext();
        vc.setId(vcId);
        vc.setType(Collections.singletonList("VerifiableCredential"));
        vc.setCredentialSchema(credentialSchema);
        vc.setCredentialSubject(credentialSubject);
        vc.setEvidence(Collections.singletonList(evidence));
        vc.setIssuer(issuer);
        vc.setIssuanceDate(now());
        vc.setValidFrom(now());
        vc.setValidUntil(nowPlusOneYear());
        vc.setEncoding("UTF-8");
        vc.setFormatVersion("1.0");
        vc.setLanguage("ko");
        vc.setProof(proof);

        return vc;
    }
}
