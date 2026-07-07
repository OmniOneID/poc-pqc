package org.omnione.did.poc.pqc;

import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.data.model.did.DidDocument;
import org.omnione.did.poc.pqc.util.DidDocumentUtil;
import org.omnione.did.poc.pqc.wallet.ECWalletKeyProvider;
import org.omnione.did.poc.pqc.wallet.MLDSAWalletKeyProvider;

/**
 * Secp256r1 vs ML-DSA-44 DID Document generation comparison demo.
 * Wallet SDK 기반 키 생성.
 */
public class DidDocMain {

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║       DID Document Comparison: Secp256r1 vs ML-DSA-44        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // ────────────────────────────────────────────
        //  1. Secp256r1 (Wallet SDK)
        // ────────────────────────────────────────────
        System.out.println("────────────────────────────────────────────");
        System.out.println("  [Secp256r1] (Wallet SDK)");
        System.out.println("────────────────────────────────────────────");

        long ecKeyGenStart = System.nanoTime();
        ECWalletKeyProvider ecAssertWallet = new ECWalletKeyProvider("assert", "123456".toCharArray());
        ECWalletKeyProvider ecAuthWallet = new ECWalletKeyProvider("auth", "123456".toCharArray());
        long ecKeyGenTime = System.nanoTime() - ecKeyGenStart;

        // Multibase 디코딩 → 순수 바이트 크기
        int ecPubKeySize = MultiBaseUtils.decode(ecAssertWallet.getPublicKeyMultibase()).length;

        DidDocument ecDidDoc = DidDocumentUtil.createDidDocument("did:omn:issuer", "did:omn:tas");
        DidDocumentUtil.addVerificationMethodWithMultibase(ecDidDoc, "assert",
                DidDocumentUtil.KEY_TYPE_SECP256R1, ecAssertWallet.getPublicKeyMultibase(), "assertionMethod");
        DidDocumentUtil.addVerificationMethodWithMultibase(ecDidDoc, "auth",
                DidDocumentUtil.KEY_TYPE_SECP256R1, ecAuthWallet.getPublicKeyMultibase(), "authentication");

        String ecDidDocJson = ecDidDoc.toJson();
        int ecDidDocSize = ecDidDocJson.getBytes().length;

        System.out.println(ecDidDocJson);
        System.out.println();

        ecAssertWallet.disconnect();
        ecAuthWallet.disconnect();

        // ────────────────────────────────────────────
        //  2. ML-DSA-44 (Wallet SDK)
        // ────────────────────────────────────────────
        System.out.println("────────────────────────────────────────────");
        System.out.println("  [ML-DSA-44] (Wallet SDK)");
        System.out.println("────────────────────────────────────────────");

        long pqcKeyGenStart = System.nanoTime();
        MLDSAWalletKeyProvider pqcAssertWallet = new MLDSAWalletKeyProvider("assert", "123456".toCharArray());
        MLDSAWalletKeyProvider pqcAuthWallet = new MLDSAWalletKeyProvider("auth", "123456".toCharArray());
        long pqcKeyGenTime = System.nanoTime() - pqcKeyGenStart;

        // Multibase 디코딩 → 순수 바이트 크기
        int pqcPubKeySize = MultiBaseUtils.decode(pqcAssertWallet.getPublicKeyMultibase()).length;

        DidDocument pqcDidDoc = DidDocumentUtil.createDidDocument("did:omn:issuer", "did:omn:tas");
        DidDocumentUtil.addVerificationMethod(pqcDidDoc, "assert",
                pqcAssertWallet.getPublicKey(), "assertionMethod");
        DidDocumentUtil.addVerificationMethod(pqcDidDoc, "auth",
                pqcAuthWallet.getPublicKey(), "authentication");

        String pqcDidDocJson = pqcDidDoc.toJson();
        int pqcDidDocSize = pqcDidDocJson.getBytes().length;

        System.out.println(pqcDidDocJson);
        System.out.println();

        pqcAssertWallet.disconnect();
        pqcAuthWallet.disconnect();

        // ────────────────────────────────────────────
        //  3. Comparison
        // ────────────────────────────────────────────
        System.out.println("╔═══════════════════╤═══════════════╤═══════════════╤══════════╗");
        System.out.println("║      Comparison: Secp256r1 vs ML-DSA-44 (DID Document)       ║");
        System.out.println("╠═══════════════════╪═══════════════╪═══════════════╪══════════╣");
        System.out.printf( "║ %-17s │ %13s │ %13s │ %8s ║%n", "Item", "Secp256r1", "ML-DSA-44", "Ratio");
        System.out.println("╠═══════════════════╪═══════════════╪═══════════════╪══════════╣");
        System.out.printf( "║ %-17s │ %,11d B │ %,11d B │ %6.1fx  ║%n",
                "Public Key Size", ecPubKeySize, pqcPubKeySize, (double) pqcPubKeySize / ecPubKeySize);
        System.out.printf( "║ %-17s │ %,11d B │ %,11d B │ %6.1fx  ║%n",
                "DID Doc Size", ecDidDocSize, pqcDidDocSize, (double) pqcDidDocSize / ecDidDocSize);
        System.out.printf( "║ %-17s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                "Key Gen (x2)", ecKeyGenTime / 1_000_000.0, pqcKeyGenTime / 1_000_000.0,
                (double) pqcKeyGenTime / ecKeyGenTime);
        System.out.println("╚═══════════════════╧═══════════════╧═══════════════╧══════════╝");
    }
}
