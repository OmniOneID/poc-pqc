package org.omnione.did.poc.pqc.core.vc;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.omnione.did.core.data.rest.ClaimInfo;
import org.omnione.did.core.data.rest.IssueVcParam;
import org.omnione.did.core.data.rest.SignatureVcParams;
import org.omnione.did.core.manager.VcManager;
import org.omnione.did.crypto.enums.MultiBaseType;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.data.model.did.DidDocument;
import org.omnione.did.data.model.enums.vc.VcType;
import org.omnione.did.data.model.provider.ProviderDetail;
import org.omnione.did.data.model.schema.VcSchema;
import org.omnione.did.data.model.vc.Claim;
import org.omnione.did.data.model.vc.VerifiableCredential;

public class OpenDidVcService {

    private static final String ISSUER_DID = "did:omn:issuer";
    private static final String ASSERT_KEY_ID = "assert";

    private final Function<byte[], byte[]> signer;
    private final DidDocument issuerDidDocument;
    private final String algorithmName;

    public OpenDidVcService(Function<byte[], byte[]> signer, DidDocument issuerDidDocument, String algorithmName) {
        this.signer = signer;
        this.issuerDidDocument = issuerDidDocument;
        this.algorithmName = algorithmName;
    }

    public VerifiableCredential issueVc(String holderDid, Map<String, String> claims) throws Exception {
        return issueVcDetailed(holderDid, claims).vc;
    }

    /**
     * VC 발급 + per-claim 서명 메트릭 반환.
     * sigVcParamsList[0] = 전체 VC proof (proofValue)
     * sigVcParamsList[1..n] = 클레임별 서명 (proofValueList)
     */
    public IssueDetailedResult issueVcDetailed(String holderDid, Map<String, String> claims) throws Exception {
        return issueVcDetailed(holderDid, claims, VcSchemaProvider.getSchema());
    }

    /** 스키마를 지정하여 VC 발급 */
    public IssueDetailedResult issueVcDetailed(String holderDid, Map<String, String> claims, String schemaJson) throws Exception {
        IssueVcParam issueVcParam = buildIssueVcParam(claims, schemaJson);

        VcManager vcManager = new VcManager();
        VerifiableCredential vc = vcManager.issueCredential(issueVcParam, holderDid);

        List<SignatureVcParams> sigVcParamsList = vcManager.getOriginDataForSign(
                ASSERT_KEY_ID, issuerDidDocument, vc);

        int totalSignCount = sigVcParamsList.size();
        int claimSignCount = totalSignCount - 1; // 첫 번째는 전체 VC proof
        long proofValueSignNanos = 0;
        int proofValueSigBytes = 0;
        long claimSignTotalNanos = 0;
        int claimSigTotalBytes = 0;

        for (int i = 0; i < sigVcParamsList.size(); i++) {
            SignatureVcParams sigVcParams = sigVcParamsList.get(i);
            byte[] originData = sigVcParams.getOriginData().getBytes(StandardCharsets.UTF_8);

            long signStart = System.nanoTime();
            byte[] signature = signer.apply(originData);
            long signTime = System.nanoTime() - signStart;

            String signatureMultibase = MultiBaseUtils.encode(signature, MultiBaseType.base64);
            sigVcParams.setSignatureValue(signatureMultibase);

            if (i == 0) { // proofValue signature
                proofValueSignNanos = signTime;
                proofValueSigBytes = signature.length;
            } else { // per-claim signature
                claimSignTotalNanos += signTime;
                claimSigTotalBytes += signature.length;
            }
        }

        vcManager.addProof(vc, sigVcParamsList);

        return new IssueDetailedResult(vc, claimSignCount, proofValueSignNanos, proofValueSigBytes, claimSignTotalNanos, claimSigTotalBytes);
    }

    public static class IssueDetailedResult {
        public final VerifiableCredential vc;
        public final int claimSignCount;
        public final long proofValueSignNanos;
        public final int proofValueSigBytes;
        public final long claimSignTotalNanos;
        public final int claimSigTotalBytes;

        public IssueDetailedResult(VerifiableCredential vc, int claimSignCount,
                                    long proofValueSignNanos, int proofValueSigBytes,
                                    long claimSignTotalNanos, int claimSigTotalBytes) {
            this.vc = vc;
            this.claimSignCount = claimSignCount;
            this.proofValueSignNanos = proofValueSignNanos;
            this.proofValueSigBytes = proofValueSigBytes;
            this.claimSignTotalNanos = claimSignTotalNanos;
            this.claimSigTotalBytes = claimSigTotalBytes;
        }
    }

    public VerificationResult verifyVc(String vcJson) throws Exception {
        return verifyVcDetailed(vcJson).toVerificationResult();
    }

    /**
     * VC 검증 + proofValue/proofValueList 개별 검증 시간 측정.
     */
    public VerifyDetailedResult verifyVcDetailed(String vcJson) throws Exception {
        // 워밍업 1회
        {
            VerifiableCredential w = new VerifiableCredential();
            w.fromJson(vcJson);
            try { new VcManager().verifyCredential(w, issuerDidDocument, true); } catch (Exception ignored) {}
        }

        // 전체 검증 (proofValue 1건 + proofValueList N건) — 3회 평균
        boolean allValid = true;
        long totalVerifyNanos = 0;
        for (int r = 0; r < 3; r++) {
            VerifiableCredential v = new VerifiableCredential();
            v.fromJson(vcJson);
            long s = System.nanoTime();
            try {
                new VcManager().verifyCredential(v, issuerDidDocument, true);
            } catch (Exception e) {
                allValid = false;
            }
            totalVerifyNanos += System.nanoTime() - s;
        }
        totalVerifyNanos /= 3;

        VerifiableCredential vc = new VerifiableCredential();
        vc.fromJson(vcJson);

        int claimCount = 0;
        Map<String, String> disclosedClaims = new LinkedHashMap<>();
        if (vc.getCredentialSubject() != null && vc.getCredentialSubject().getClaims() != null) {
            for (Claim claim : vc.getCredentialSubject().getClaims()) {
                disclosedClaims.put(claim.getCode(), claim.getValue());
                claimCount++;
            }
        }

        // verifyCredential은 proofValue(1건) + proofValueList(N건) = 1+N건 동일 알고리즘 검증
        // per-signature ≈ total / (1 + N), claim verify total ≈ per-signature * N
        long perSigNanos = claimCount > 0 ? totalVerifyNanos / (1 + claimCount) : 0;
        long claimVerifyNanos = perSigNanos * claimCount;

        return new VerifyDetailedResult(allValid, algorithmName, disclosedClaims,
                totalVerifyNanos, claimVerifyNanos, claimCount);
    }

    public static class VerifyDetailedResult {
        public final boolean valid;
        public final String algorithm;
        public final Map<String, String> claims;
        public final long totalVerifyNanos;
        public final long claimVerifyNanos;
        public final int claimCount;

        public VerifyDetailedResult(boolean valid, String algorithm, Map<String, String> claims,
                                     long totalVerifyNanos, long claimVerifyNanos, int claimCount) {
            this.valid = valid;
            this.algorithm = algorithm;
            this.claims = claims;
            this.totalVerifyNanos = totalVerifyNanos;
            this.claimVerifyNanos = claimVerifyNanos;
            this.claimCount = claimCount;
        }

        public VerificationResult toVerificationResult() {
            return new VerificationResult(valid, algorithm, claims);
        }
    }

    private IssueVcParam buildIssueVcParam(Map<String, String> claims) {
        return buildIssueVcParam(claims, VcSchemaProvider.getSchema());
    }

    private IssueVcParam buildIssueVcParam(Map<String, String> claims, String schemaJson) {
        IssueVcParam param = new IssueVcParam();

        VcSchema vcSchema = new VcSchema();
        vcSchema.fromJson(schemaJson);
        param.setVcSchema(vcSchema);

        ProviderDetail providerDetail = new ProviderDetail();
        providerDetail.setDid(ISSUER_DID);
        providerDetail.setName("PQC PoC Issuer");
        providerDetail.setCertVcRef("https://example.com/cert-vc");
        param.setProviderDetail(providerDetail);

        HashMap<String, ClaimInfo> claimInfoMap = new HashMap<>();
        for (Map.Entry<String, String> entry : claims.entrySet()) {
            ClaimInfo claimInfo = new ClaimInfo();
            claimInfo.setCode(VcSchemaProvider.NAMESPACE_ID + "." + entry.getKey());
            claimInfo.setValue(entry.getValue().getBytes(StandardCharsets.UTF_8));
            claimInfoMap.put(claimInfo.getCode(), claimInfo);
        }
        param.setPrivacy(claimInfoMap);

        param.setVcType(List.of(VcType.VERIFIABLE_CREDENTIAL));

        param.setValidFrom(ZonedDateTime.now(ZoneOffset.UTC));
        param.setValidUntil(ZonedDateTime.now(ZoneOffset.UTC).plusYears(1));

        return param;
    }

    public static class VerificationResult {
        private final boolean valid;
        private final String algorithm;
        private final Map<String, String> claims;

        public VerificationResult(boolean valid, String algorithm, Map<String, String> claims) {
            this.valid = valid;
            this.algorithm = algorithm;
            this.claims = claims;
        }

        public boolean isValid() { return valid; }
        public String getAlgorithm() { return algorithm; }
        public Map<String, String> getClaims() { return claims; }
    }
}
