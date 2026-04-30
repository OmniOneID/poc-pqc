package org.omnione.did.poc.pqc;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.omnione.did.data.model.did.DidDocument;
import org.omnione.did.data.model.vc.VerifiableCredential;
import org.omnione.did.poc.pqc.core.vc.OpenDidVcService;
import org.omnione.did.poc.pqc.core.vc.OpenDidVcService.IssueDetailedResult;
import org.omnione.did.poc.pqc.core.vc.OpenDidVcService.VerificationResult;
import org.omnione.did.poc.pqc.core.vc.OpenDidVcService.VerifyDetailedResult;
import org.omnione.did.poc.pqc.core.vc.VcSchemaProvider;
import org.omnione.did.poc.pqc.util.DidDocumentUtil;
import org.omnione.did.poc.pqc.wallet.EcWalletKeyProvider;
import org.omnione.did.poc.pqc.wallet.WalletKeyProvider;

/**
 * Secp256r1 vs ML-DSA-44 Open DID VC Issue/Verify comparison demo.
 * Section 1-3: proofValue (전체 VC) — 텍스트 10개 클레임
 * Section 4:   proofValueList (클레임 단위 개별 서명) — 텍스트 10개 클레임
 * Section 5:   proofValueList (클레임 단위 개별 서명) — 텍스트 10개 + JPG 이미지 1개 (11 claims)
 */
public class VcMain {

    private static final String ISSUER_DID = "did:omn:issuer";
    private static final String ASSERT_KEY_ID = "assert";
    private static final String HOLDER_DID = "did:omn:holder";

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║      VC Issue/Verify Comparison: Secp256r1 vs ML-DSA-44      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // 텍스트 클레임 10개 (Section 1-3용)
        Map<String, String> textClaims = new LinkedHashMap<>();
        textClaims.put("given_name", "Gil-dong");
        textClaims.put("family_name", "Hong");
        textClaims.put("birthdate", "1990-01-01");
        textClaims.put("national_id", "900101-1234567");
        textClaims.put("address", "Seoul, Gangnam-gu, Teheran-ro 123");
        textClaims.put("phone", "+82-10-1234-5678");
        textClaims.put("email", "gildong@example.com");
        textClaims.put("gender", "Male");
        textClaims.put("nationality", "KR");
        textClaims.put("issue_date", "2026-03-16");

        // 텍스트 10개 + JPG 이미지 1개 (Section 4 proofValueList용)
        Map<String, String> allClaims = new LinkedHashMap<>(textClaims);
        allClaims.put("photo", Base64.getEncoder().encodeToString(generateDummyJpeg(300 * 1024)));

        // ────────────────────────────────────────────
        //  1. Secp256r1 (텍스트 10 claims)
        // ────────────────────────────────────────────
        System.out.println("────────────────────────────────────────────");
        System.out.println("  [Secp256r1]");
        System.out.println("────────────────────────────────────────────");

        long ecKeyGenStart = System.nanoTime();
        EcWalletKeyProvider ecWallet = new EcWalletKeyProvider(ASSERT_KEY_ID, "123456".toCharArray());
        long ecKeyGenTime = System.nanoTime() - ecKeyGenStart;

        DidDocument ecDidDoc = DidDocumentUtil.createDidDocument(ISSUER_DID, ISSUER_DID);
        DidDocumentUtil.addVerificationMethodWithMultibase(ecDidDoc, ASSERT_KEY_ID,
                DidDocumentUtil.KEY_TYPE_SECP256R1, ecWallet.getPublicKeyMultibase(), "assertionMethod");

        OpenDidVcService ecVcService = new OpenDidVcService(
                data -> { try { return ecWallet.sign(data); } catch (Exception e) { throw new RuntimeException(e); } },
                ecDidDoc, "Secp256r1");

        ecVcService.issueVcDetailed(HOLDER_DID, textClaims); // warmup

        IssueDetailedResult ecIssueResult = ecVcService.issueVcDetailed(HOLDER_DID, textClaims);

        VerifiableCredential ecVc = ecIssueResult.vc;
        String ecVcJson = ecVc.toJson();
        int ecVcSize = ecVcJson.getBytes().length;

        System.out.println(ecVcJson);
        System.out.println();

        long ecVerifyStart = System.nanoTime();
        VerificationResult ecResult = ecVcService.verifyVc(ecVcJson);
        long ecVerifyTime = System.nanoTime() - ecVerifyStart;

        System.out.println("  Algorithm       : " + ecResult.getAlgorithm());
        System.out.println("  Verify          : " + (ecResult.isValid() ? "OK" : "FAIL"));
        System.out.println("  Claim Sign Count: " + ecIssueResult.claimSignCount);
        System.out.println();

        // ────────────────────────────────────────────
        //  2. ML-DSA-44 (텍스트 10 claims)
        // ────────────────────────────────────────────
        System.out.println("────────────────────────────────────────────");
        System.out.println("  [ML-DSA-44]");
        System.out.println("────────────────────────────────────────────");

        long pqcKeyGenStart = System.nanoTime();
        WalletKeyProvider pqcWallet = new WalletKeyProvider(ASSERT_KEY_ID, "123456".toCharArray());
        long pqcKeyGenTime = System.nanoTime() - pqcKeyGenStart;

        DidDocument pqcDidDoc = DidDocumentUtil.createDidDocument(ISSUER_DID, ISSUER_DID);
        DidDocumentUtil.addVerificationMethod(pqcDidDoc, ASSERT_KEY_ID,
                pqcWallet.getPublicKey(), "assertionMethod");

        OpenDidVcService pqcVcService = new OpenDidVcService(
                data -> { try { return pqcWallet.sign(data); } catch (Exception e) { throw new RuntimeException(e); } },
                pqcDidDoc, "ML-DSA-44");

        pqcVcService.issueVcDetailed(HOLDER_DID, textClaims); // warmup

        IssueDetailedResult pqcIssueResult = pqcVcService.issueVcDetailed(HOLDER_DID, textClaims);

        VerifiableCredential pqcVc = pqcIssueResult.vc;
        String pqcVcJson = pqcVc.toJson();
        int pqcVcSize = pqcVcJson.getBytes().length;

        System.out.println(pqcVcJson);
        System.out.println();

        long pqcVerifyStart = System.nanoTime();
        VerificationResult pqcResult = pqcVcService.verifyVc(pqcVcJson);
        long pqcVerifyTime = System.nanoTime() - pqcVerifyStart;

        System.out.println("  Algorithm       : " + pqcResult.getAlgorithm());
        System.out.println("  Verify          : " + (pqcResult.isValid() ? "OK" : "FAIL"));
        System.out.println("  Claim Sign Count: " + pqcIssueResult.claimSignCount);
        System.out.println();

        // ────────────────────────────────────────────
        //  3. VC Comparison (proofValue) — 텍스트 10 claims
        // ────────────────────────────────────────────
        int ecProofSize = ecIssueResult.proofValueSigBytes;
        int pqcProofSize = pqcIssueResult.proofValueSigBytes;

        System.out.println("╔═══════════════════╤═══════════════╤═══════════════╤══════════╗");
        System.out.println("║     Comparison: Secp256r1 vs ML-DSA-44 (VC, 10 text claims)  ║");
        System.out.println("╠═══════════════════╪═══════════════╪═══════════════╪══════════╣");
        System.out.printf( "║ %-17s │ %13s │ %13s │ %8s ║%n", "Item", "Secp256r1", "ML-DSA-44", "Ratio");
        System.out.println("╠═══════════════════╪═══════════════╪═══════════════╪══════════╣");
        System.out.printf( "║ %-17s │ %,11d B │ %,11d B │ %6.1fx  ║%n",
                "VC JSON Size", ecVcSize, pqcVcSize, (double) pqcVcSize / ecVcSize);
        System.out.printf( "║ %-17s │ %,11d B │ %,11d B │ %6.1fx  ║%n",
                "proofValue (raw)", ecProofSize, pqcProofSize,
                ecProofSize > 0 ? (double) pqcProofSize / ecProofSize : 0);
        System.out.printf( "║ %-17s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                "Key Generation", ecKeyGenTime / 1_000_000.0, pqcKeyGenTime / 1_000_000.0,
                (double) pqcKeyGenTime / ecKeyGenTime);
        System.out.printf( "║ %-17s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                "VC Sign Only", ecIssueResult.proofValueSignNanos / 1_000_000.0,
                pqcIssueResult.proofValueSignNanos / 1_000_000.0,
                (double) pqcIssueResult.proofValueSignNanos / ecIssueResult.proofValueSignNanos);
        System.out.printf( "║ %-17s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                "VC Verify", ecVerifyTime / 1_000_000.0, pqcVerifyTime / 1_000_000.0,
                (double) pqcVerifyTime / ecVerifyTime);
        System.out.println("╚═══════════════════╧═══════════════╧═══════════════╧══════════╝");

        // ────────────────────────────────────────────
        //  4. proofValueList Comparison (텍스트 10 claims only)
        // ────────────────────────────────────────────
        System.out.println();
        System.out.println("────────────────────────────────────────────");
        System.out.println("  proofValueList: Issuing VC with 10 text claims");
        System.out.println("────────────────────────────────────────────");

        // 이미 section 1-2에서 발급한 VC를 재활용 (textClaims, 기본 스키마)
        System.out.println("  [Secp256r1] VC JSON:");
        System.out.println(ecVcJson);
        System.out.println();
        System.out.println("  [ML-DSA-44] VC JSON:");
        System.out.println(pqcVcJson);
        System.out.println();

        VerifyDetailedResult ecTextVerify = ecVcService.verifyVcDetailed(ecVcJson);
        System.out.println("  [Secp256r1] 10-claim VC Verify: " + (ecTextVerify.valid ? "OK" : "FAIL"));
        VerifyDetailedResult pqcTextVerify = pqcVcService.verifyVcDetailed(pqcVcJson);
        System.out.println("  [ML-DSA-44] 10-claim VC Verify: " + (pqcTextVerify.valid ? "OK" : "FAIL"));
        System.out.println();

        {
            int ecClaimCount = ecIssueResult.claimSignCount;
            int pqcClaimCount = pqcIssueResult.claimSignCount;
            int ecPerClaimSigBytes = ecClaimCount > 0 ? ecIssueResult.claimSigTotalBytes / ecClaimCount : 0;
            int pqcPerClaimSigBytes = pqcClaimCount > 0 ? pqcIssueResult.claimSigTotalBytes / pqcClaimCount : 0;
            double ecPerClaimSignMs = ecClaimCount > 0 ? ecIssueResult.claimSignTotalNanos / 1_000_000.0 / ecClaimCount : 0;
            double pqcPerClaimSignMs = pqcClaimCount > 0 ? pqcIssueResult.claimSignTotalNanos / 1_000_000.0 / pqcClaimCount : 0;
            double ecPerClaimVerifyMs = ecTextVerify.claimCount > 0 ? ecTextVerify.claimVerifyNanos / 1_000_000.0 / ecTextVerify.claimCount : 0;
            double pqcPerClaimVerifyMs = pqcTextVerify.claimCount > 0 ? pqcTextVerify.claimVerifyNanos / 1_000_000.0 / pqcTextVerify.claimCount : 0;
            System.out.println("╔══════════════════════════╤═══════════════╤═══════════════╤══════════╗");
            System.out.println("║  proofValueList: Per-Claim Signatures (10 text claims, x" + ecClaimCount + ")         ║");
            System.out.println("╠══════════════════════════╪═══════════════╪═══════════════╪══════════╣");
            System.out.printf( "║ %-24s │ %13s │ %13s │ %8s ║%n", "Item", "Secp256r1", "ML-DSA-44", "Ratio");
            System.out.println("╠══════════════════════════╪═══════════════╪═══════════════╪══════════╣");
            System.out.printf( "║ %-24s │ %,11d B │ %,11d B │ %6.1fx  ║%n",
                    "VC JSON Size", ecVcSize, pqcVcSize, (double) pqcVcSize / ecVcSize);
            System.out.printf( "║ %-24s │ %,11d B │ %,11d B │ %6.1fx  ║%n",
                    "Per-Claim Sig (raw)", ecPerClaimSigBytes, pqcPerClaimSigBytes,
                    ecPerClaimSigBytes > 0 ? (double) pqcPerClaimSigBytes / ecPerClaimSigBytes : 0);
            System.out.printf( "║ %-24s │ %,11d B │ %,11d B │ %6.1fx  ║%n",
                    "Total Claim Sigs (raw)", ecIssueResult.claimSigTotalBytes, pqcIssueResult.claimSigTotalBytes,
                    ecIssueResult.claimSigTotalBytes > 0 ? (double) pqcIssueResult.claimSigTotalBytes / ecIssueResult.claimSigTotalBytes : 0);
            System.out.println("╠══════════════════════════╪═══════════════╪═══════════════╪══════════╣");
            System.out.printf( "║ %-24s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                    "Per-Claim Sign (avg)", ecPerClaimSignMs, pqcPerClaimSignMs,
                    ecPerClaimSignMs > 0 ? pqcPerClaimSignMs / ecPerClaimSignMs : 0);
            System.out.printf( "║ %-24s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                    "Total Claim Sign", ecIssueResult.claimSignTotalNanos / 1_000_000.0,
                    pqcIssueResult.claimSignTotalNanos / 1_000_000.0,
                    ecIssueResult.claimSignTotalNanos > 0
                            ? (double) pqcIssueResult.claimSignTotalNanos / ecIssueResult.claimSignTotalNanos : 0);
            System.out.printf( "║ %-24s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                    "Per-Claim Verify (avg)", ecPerClaimVerifyMs, pqcPerClaimVerifyMs,
                    ecPerClaimVerifyMs > 0 ? pqcPerClaimVerifyMs / ecPerClaimVerifyMs : 0);
            System.out.printf( "║ %-24s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                    "Total Claim Verify", ecTextVerify.claimVerifyNanos / 1_000_000.0,
                    pqcTextVerify.claimVerifyNanos / 1_000_000.0,
                    ecTextVerify.claimVerifyNanos > 0
                            ? (double) pqcTextVerify.claimVerifyNanos / ecTextVerify.claimVerifyNanos : 0);
            System.out.println("╚══════════════════════════╧═══════════════╧═══════════════╧══════════╝");
        }

        // ────────────────────────────────────────────
        //  5. proofValueList Comparison (텍스트 10 + JPG 이미지 1 = 11 claims)
        // ────────────────────────────────────────────
        System.out.println();
        System.out.println("────────────────────────────────────────────");
        System.out.println("  proofValueList: Issuing VC with 11 claims (10 text + 1 JPG ~300KB)");
        System.out.println("────────────────────────────────────────────");

        String photoSchema = VcSchemaProvider.getWithPhotoSchema();

        IssueDetailedResult ecPvlResult = ecVcService.issueVcDetailed(HOLDER_DID, allClaims, photoSchema);
        String ecPvlJson = ecPvlResult.vc.toJson();
        int ecPvlVcSize = ecPvlJson.getBytes().length;
        // System.out.println("  [Secp256r1] VC JSON:");
        // System.out.println(ecPvlJson);
        // System.out.println();

        IssueDetailedResult pqcPvlResult = pqcVcService.issueVcDetailed(HOLDER_DID, allClaims, photoSchema);
        String pqcPvlJson = pqcPvlResult.vc.toJson();
        int pqcPvlVcSize = pqcPvlJson.getBytes().length;
        // System.out.println("  [ML-DSA-44] VC JSON:");
        // System.out.println(pqcPvlJson);
        // System.out.println();

        VerifyDetailedResult ecPvlVerify = ecVcService.verifyVcDetailed(ecPvlJson);
        System.out.println("  [Secp256r1] 11-claim VC Verify: " + (ecPvlVerify.valid ? "OK" : "FAIL"));
        VerifyDetailedResult pqcPvlVerify = pqcVcService.verifyVcDetailed(pqcPvlJson);
        System.out.println("  [ML-DSA-44] 11-claim VC Verify: " + (pqcPvlVerify.valid ? "OK" : "FAIL"));
        System.out.println();

        ecWallet.disconnect();
        pqcWallet.disconnect();

        {
            int ecClaimCount = ecPvlResult.claimSignCount;
            int pqcClaimCount = pqcPvlResult.claimSignCount;
            int ecPerClaimSigBytes = ecClaimCount > 0 ? ecPvlResult.claimSigTotalBytes / ecClaimCount : 0;
            int pqcPerClaimSigBytes = pqcClaimCount > 0 ? pqcPvlResult.claimSigTotalBytes / pqcClaimCount : 0;
            double ecPerClaimSignMs = ecClaimCount > 0 ? ecPvlResult.claimSignTotalNanos / 1_000_000.0 / ecClaimCount : 0;
            double pqcPerClaimSignMs = pqcClaimCount > 0 ? pqcPvlResult.claimSignTotalNanos / 1_000_000.0 / pqcClaimCount : 0;
            double ecPerClaimVerifyMs = ecPvlVerify.claimCount > 0 ? ecPvlVerify.claimVerifyNanos / 1_000_000.0 / ecPvlVerify.claimCount : 0;
            double pqcPerClaimVerifyMs = pqcPvlVerify.claimCount > 0 ? pqcPvlVerify.claimVerifyNanos / 1_000_000.0 / pqcPvlVerify.claimCount : 0;
            System.out.println("╔══════════════════════════╤═══════════════╤═══════════════╤══════════╗");
            System.out.println("║  proofValueList: Per-Claim Signatures (11 claims, incl. JPG, x" + ecClaimCount + ")   ║");
            System.out.println("╠══════════════════════════╪═══════════════╪═══════════════╪══════════╣");
            System.out.printf( "║ %-24s │ %13s │ %13s │ %8s ║%n", "Item", "Secp256r1", "ML-DSA-44", "Ratio");
            System.out.println("╠══════════════════════════╪═══════════════╪═══════════════╪══════════╣");
            System.out.printf( "║ %-24s │ %,11d B │ %,11d B │ %6.1fx  ║%n",
                    "VC JSON Size", ecPvlVcSize, pqcPvlVcSize, (double) pqcPvlVcSize / ecPvlVcSize);
            System.out.printf( "║ %-24s │ %,11d B │ %,11d B │ %6.1fx  ║%n",
                    "Per-Claim Sig (raw)", ecPerClaimSigBytes, pqcPerClaimSigBytes,
                    ecPerClaimSigBytes > 0 ? (double) pqcPerClaimSigBytes / ecPerClaimSigBytes : 0);
            System.out.printf( "║ %-24s │ %,11d B │ %,11d B │ %6.1fx  ║%n",
                    "Total Claim Sigs (raw)", ecPvlResult.claimSigTotalBytes, pqcPvlResult.claimSigTotalBytes,
                    ecPvlResult.claimSigTotalBytes > 0 ? (double) pqcPvlResult.claimSigTotalBytes / ecPvlResult.claimSigTotalBytes : 0);
            System.out.println("╠══════════════════════════╪═══════════════╪═══════════════╪══════════╣");
            System.out.printf( "║ %-24s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                    "Per-Claim Sign (avg)", ecPerClaimSignMs, pqcPerClaimSignMs,
                    ecPerClaimSignMs > 0 ? pqcPerClaimSignMs / ecPerClaimSignMs : 0);
            System.out.printf( "║ %-24s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                    "Total Claim Sign", ecPvlResult.claimSignTotalNanos / 1_000_000.0,
                    pqcPvlResult.claimSignTotalNanos / 1_000_000.0,
                    ecPvlResult.claimSignTotalNanos > 0
                            ? (double) pqcPvlResult.claimSignTotalNanos / ecPvlResult.claimSignTotalNanos : 0);
            System.out.printf( "║ %-24s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                    "Per-Claim Verify (avg)", ecPerClaimVerifyMs, pqcPerClaimVerifyMs,
                    ecPerClaimVerifyMs > 0 ? pqcPerClaimVerifyMs / ecPerClaimVerifyMs : 0);
            System.out.printf( "║ %-24s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                    "Total Claim Verify", ecPvlVerify.claimVerifyNanos / 1_000_000.0,
                    pqcPvlVerify.claimVerifyNanos / 1_000_000.0,
                    ecPvlVerify.claimVerifyNanos > 0
                            ? (double) pqcPvlVerify.claimVerifyNanos / ecPvlVerify.claimVerifyNanos : 0);
            System.out.println("╚══════════════════════════╧═══════════════╧═══════════════╧══════════╝");
        }
    }

    private static String extractProofValue(String vcJson) {
        String key = "\"proofValue\":\"";
        int start = vcJson.indexOf(key);
        if (start < 0) {
            key = "\"proofValue\": \"";
            start = vcJson.indexOf(key);
        }
        if (start < 0) return null;
        start += key.length();
        int end = vcJson.indexOf("\"", start);
        return end > start ? vcJson.substring(start, end) : null;
    }

    /**
     * proofValueList 배열에서 각 항목의 문자열 길이를 추출.
     */
    private static int[] extractProofValueListLengths(String vcJson) {
        String key = "\"proofValueList\":[";
        int start = vcJson.indexOf(key);
        if (start < 0) {
            key = "\"proofValueList\": [";
            start = vcJson.indexOf(key);
        }
        if (start < 0) return new int[0];
        start += key.length();
        int end = vcJson.indexOf("]", start);
        if (end < 0) return new int[0];

        String listContent = vcJson.substring(start, end);
        // "value1","value2","value3" 형태에서 각 value 길이 추출
        String[] items = listContent.split(",");
        int[] lengths = new int[items.length];
        for (int i = 0; i < items.length; i++) {
            String item = items[i].trim();
            // 따옴표 제거
            if (item.startsWith("\"") && item.endsWith("\"")) {
                lengths[i] = item.length() - 2;
            } else {
                lengths[i] = item.length();
            }
        }
        return lengths;
    }

    /**
     * 유효한 JPEG 구조의 더미 이미지 생성 (~targetSize 바이트).
     * SOI + APP0(JFIF) + COM 세그먼트 패딩 + EOI
     */
    private static byte[] generateDummyJpeg(int targetSize) {
        byte[] header = {
            (byte)0xFF, (byte)0xD8,                     // SOI
            (byte)0xFF, (byte)0xE0,                     // APP0
            0x00, 0x10,                                  // Length=16
            0x4A, 0x46, 0x49, 0x46, 0x00,               // "JFIF\0"
            0x01, 0x01,                                  // Version 1.1
            0x00,                                        // Aspect ratio units
            0x00, 0x01, 0x00, 0x01,                     // X/Y density = 1
            0x00, 0x00                                   // No thumbnail
        };
        byte[] footer = { (byte)0xFF, (byte)0xD9 };    // EOI

        byte[] result = new byte[targetSize];
        System.arraycopy(header, 0, result, 0, header.length);

        int pos = header.length;
        Random rng = new Random(42);
        while (pos < targetSize - footer.length) {
            int remaining = targetSize - footer.length - pos;
            if (remaining < 4) break;
            int segDataLen = Math.min(remaining - 2, 65533);
            result[pos++] = (byte)0xFF;
            result[pos++] = (byte)0xFE; // COM
            result[pos++] = (byte)((segDataLen >> 8) & 0xFF);
            result[pos++] = (byte)(segDataLen & 0xFF);
            int dataBytes = segDataLen - 2;
            for (int i = 0; i < dataBytes && pos < targetSize - footer.length; i++) {
                result[pos++] = (byte)(rng.nextInt(254) + 1);
            }
        }

        System.arraycopy(footer, 0, result, targetSize - footer.length, footer.length);
        return result;
    }
}
