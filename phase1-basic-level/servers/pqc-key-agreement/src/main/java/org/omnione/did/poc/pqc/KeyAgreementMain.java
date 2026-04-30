package org.omnione.did.poc.pqc;

import java.security.SecureRandom;
import java.util.HexFormat;

import org.omnione.did.data.model.did.DidDocument;
import org.omnione.did.poc.pqc.crypto.keyagree.ECDHProtocol;
import org.omnione.did.poc.pqc.crypto.keyagree.ECDHProtocol.ECDHExchangeResult;
import org.omnione.did.poc.pqc.crypto.keyagree.ECDHProtocol.ReqECDHResult;
import org.omnione.did.poc.pqc.crypto.keyagree.MLKEMProtocol;
import org.omnione.did.poc.pqc.crypto.keyagree.MLKEMProtocol.MLKEMExchangeResult;
import org.omnione.did.poc.pqc.crypto.keyagree.MLKEMProtocol.ReqMLKEMResult;
import org.omnione.did.poc.pqc.util.DidDocumentUtil;
import org.omnione.did.poc.pqc.wallet.EcWalletKeyProvider;
import org.omnione.did.poc.pqc.wallet.WalletKeyProvider;

/**
 * Key Agreement Comparison: ECDH(Secp256r1) vs ML-KEM-768 demo.
 * Wallet SDK 기반 키 생성/서명.
 */
public class KeyAgreementMain {

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║    Key Agreement Comparison: ECDH(Secp256r1) vs ML-KEM-768   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        HexFormat hex = HexFormat.of();

        byte[] serverNonce = new byte[16];
        new SecureRandom().nextBytes(serverNonce);
        System.out.println("  serverNonce: " + hex.formatHex(serverNonce));
        System.out.println();

        // ════════════════════════════════════════════
        //  1. ECDH (Secp256r1) - Wallet SDK
        // ════════════════════════════════════════════
        System.out.println("────────────────────────────────────────────");
        System.out.println("  [ECDH / Secp256r1] (Wallet SDK)");
        System.out.println("────────────────────────────────────────────");

        ECDHProtocol ecdhProtocol = new ECDHProtocol();

        long ecSetupStart = System.nanoTime();
        EcWalletKeyProvider ecAliceWallet = new EcWalletKeyProvider("keyagree", "alice123".toCharArray());
        EcWalletKeyProvider ecBobWallet = new EcWalletKeyProvider("keyagree", "bob123".toCharArray());

        DidDocument ecAliceDidDoc = DidDocumentUtil.createDidDocument("did:omn:alice", "did:omn:tas");
        DidDocumentUtil.addVerificationMethodWithMultibase(ecAliceDidDoc, "keyagree",
                DidDocumentUtil.KEY_TYPE_SECP256R1, ecAliceWallet.getPublicKeyMultibase(), "keyAgreement");

        DidDocument ecBobDidDoc = DidDocumentUtil.createDidDocument("did:omn:bob", "did:omn:tas");
        DidDocumentUtil.addVerificationMethodWithMultibase(ecBobDidDoc, "keyagree",
                DidDocumentUtil.KEY_TYPE_SECP256R1, ecBobWallet.getPublicKeyMultibase(), "keyAgreement");
        long ecSetupTime = System.nanoTime() - ecSetupStart;

        // Step 1: Alice -> reqECDH (wallet signer)
        long ecStep1Start = System.nanoTime();
        ReqECDHResult ecReqResult = ecdhProtocol.createReqECDH(ecAliceDidDoc,
                data -> { try { return ecAliceWallet.sign(data); } catch (Exception e) { throw new RuntimeException(e); } });
        long ecStep1Time = System.nanoTime() - ecStep1Start;

        String ecReqJson = ecReqResult.reqECDH().toJson();
        int ecReqSize = ecReqJson.getBytes().length;
        System.out.println("  reqECDH:");
        System.out.println(ecReqJson);
        System.out.println();

        // Step 2: Bob -> resECDH (wallet signer)
        long ecStep2Start = System.nanoTime();
        ECDHExchangeResult ecExResult = ecdhProtocol.processReqECDH(
                ecReqResult.reqECDH(), ecAliceDidDoc, ecBobDidDoc,
                data -> { try { return ecBobWallet.sign(data); } catch (Exception e) { throw new RuntimeException(e); } },
                serverNonce);
        long ecStep2Time = System.nanoTime() - ecStep2Start;

        byte[] ecBobSessionKey = ecExResult.sessionKey();
        String ecResJson = ecExResult.resECDH().toJson();
        int ecResSize = ecResJson.getBytes().length;
        System.out.println("  resECDH:");
        System.out.println(ecResJson);
        System.out.println();

        // Step 3: Alice -> sessionKey
        byte[] ecClientNonce = java.util.Base64.getDecoder().decode(
                ecReqResult.reqECDH().getNonce().substring(1));
        long ecStep3Start = System.nanoTime();
        byte[] ecAliceSessionKey = ecdhProtocol.processResECDH(
                ecExResult.resECDH(), ecBobDidDoc,
                ecReqResult.ephemeralKeyPair().getPrivate(), ecClientNonce, serverNonce);
        long ecStep3Time = System.nanoTime() - ecStep3Start;

        long ecTotalTime = ecStep1Time + ecStep2Time + ecStep3Time;

        boolean ecMatch = hex.formatHex(ecBobSessionKey).equals(hex.formatHex(ecAliceSessionKey));
        System.out.println("  Session key " + (ecMatch ? "MATCH!" : "MISMATCH!"));
        System.out.println("  sessionKey: " + hex.formatHex(ecAliceSessionKey));
        System.out.println();

        ecAliceWallet.disconnect();
        ecBobWallet.disconnect();

        // ════════════════════════════════════════════
        //  2. ML-KEM-768 - Wallet SDK
        // ════════════════════════════════════════════
        System.out.println("────────────────────────────────────────────");
        System.out.println("  [ML-KEM-768 / ML-DSA-44] (Wallet SDK)");
        System.out.println("────────────────────────────────────────────");

        MLKEMProtocol mlkemProtocol = new MLKEMProtocol();

        long pqcSetupStart = System.nanoTime();
        WalletKeyProvider pqcAliceWallet = new WalletKeyProvider("keyagree", "alice123".toCharArray());
        WalletKeyProvider pqcBobWallet = new WalletKeyProvider("keyagree", "bob123".toCharArray());

        DidDocument pqcAliceDidDoc = DidDocumentUtil.createDidDocument("did:omn:alice", "did:omn:tas");
        DidDocumentUtil.addVerificationMethod(pqcAliceDidDoc, "keyagree",
                pqcAliceWallet.getPublicKey(), "keyAgreement");

        DidDocument pqcBobDidDoc = DidDocumentUtil.createDidDocument("did:omn:bob", "did:omn:tas");
        DidDocumentUtil.addVerificationMethod(pqcBobDidDoc, "keyagree",
                pqcBobWallet.getPublicKey(), "keyAgreement");
        long pqcSetupTime = System.nanoTime() - pqcSetupStart;

        // Step 1: Alice -> reqMLKEM (wallet signer)
        long pqcStep1Start = System.nanoTime();
        ReqMLKEMResult pqcReqResult = mlkemProtocol.createReqMLKEM(pqcAliceDidDoc,
                data -> { try { return pqcAliceWallet.sign(data); } catch (Exception e) { throw new RuntimeException(e); } });
        long pqcStep1Time = System.nanoTime() - pqcStep1Start;

        String pqcReqJson = pqcReqResult.reqMLKEM().toJson();
        int pqcReqSize = pqcReqJson.getBytes().length;
        System.out.println("  reqMLKEM:");
        System.out.println(pqcReqJson);
        System.out.println();

        // Step 2: Bob -> resMLKEM (wallet signer)
        long pqcStep2Start = System.nanoTime();
        MLKEMExchangeResult pqcExResult = mlkemProtocol.processReqMLKEM(
                pqcReqResult.reqMLKEM(), pqcAliceDidDoc, pqcBobDidDoc,
                data -> { try { return pqcBobWallet.sign(data); } catch (Exception e) { throw new RuntimeException(e); } },
                serverNonce);
        long pqcStep2Time = System.nanoTime() - pqcStep2Start;

        byte[] pqcBobSessionKey = pqcExResult.sessionKey();
        String pqcResJson = pqcExResult.resMLKEM().toJson();
        int pqcResSize = pqcResJson.getBytes().length;
        System.out.println("  resMLKEM:");
        System.out.println(pqcResJson);
        System.out.println();

        // Step 3: Alice -> sessionKey
        byte[] pqcClientNonce = java.util.Base64.getDecoder().decode(
                pqcReqResult.reqMLKEM().getNonce().substring(1));
        long pqcStep3Start = System.nanoTime();
        byte[] pqcAliceSessionKey = mlkemProtocol.processResMLKEM(
                pqcExResult.resMLKEM(), pqcBobDidDoc,
                pqcReqResult.ephemeralPrivateKey(), pqcClientNonce, serverNonce);
        long pqcStep3Time = System.nanoTime() - pqcStep3Start;

        long pqcTotalTime = pqcStep1Time + pqcStep2Time + pqcStep3Time;

        boolean pqcMatch = hex.formatHex(pqcBobSessionKey).equals(hex.formatHex(pqcAliceSessionKey));
        System.out.println("  Session key " + (pqcMatch ? "MATCH!" : "MISMATCH!"));
        System.out.println("  sessionKey: " + hex.formatHex(pqcAliceSessionKey));
        System.out.println();

        pqcAliceWallet.disconnect();
        pqcBobWallet.disconnect();

        // ════════════════════════════════════════════
        //  3. Comparison
        // ════════════════════════════════════════════
        System.out.println("╔═══════════════════╤═══════════════╤═══════════════╤══════════╗");
        System.out.println("║          Comparison: ECDH(Secp256r1) vs ML-KEM-768           ║");
        System.out.println("╠═══════════════════╪═══════════════╪═══════════════╪══════════╣");
        System.out.printf( "║ %-17s │ %13s │ %13s │ %8s ║%n", "Item", "ECDH", "ML-KEM-768", "Ratio");
        System.out.println("╠═══════════════════╪═══════════════╪═══════════════╪══════════╣");
        System.out.printf( "║ %-17s │ %,11d B │ %,11d B │ %6.1fx  ║%n",
                "Request Size", ecReqSize, pqcReqSize, (double) pqcReqSize / ecReqSize);
        System.out.printf( "║ %-17s │ %,11d B │ %,11d B │ %6.1fx  ║%n",
                "Response Size", ecResSize, pqcResSize, (double) pqcResSize / ecResSize);
        System.out.printf( "║ %-17s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                "DID Setup", ecSetupTime / 1_000_000.0, pqcSetupTime / 1_000_000.0,
                (double) pqcSetupTime / ecSetupTime);
        System.out.printf( "║ %-17s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                "Step1 (Request)", ecStep1Time / 1_000_000.0, pqcStep1Time / 1_000_000.0,
                (double) pqcStep1Time / ecStep1Time);
        System.out.printf( "║ %-17s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                "Step2 (Response)", ecStep2Time / 1_000_000.0, pqcStep2Time / 1_000_000.0,
                (double) pqcStep2Time / ecStep2Time);
        System.out.printf( "║ %-17s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                "Step3 (Derive)", ecStep3Time / 1_000_000.0, pqcStep3Time / 1_000_000.0,
                (double) pqcStep3Time / ecStep3Time);
        System.out.printf( "║ %-17s │ %10.3f ms │ %10.3f ms │ %6.1fx  ║%n",
                "Total Protocol", ecTotalTime / 1_000_000.0, pqcTotalTime / 1_000_000.0,
                (double) pqcTotalTime / ecTotalTime);
        System.out.println("╠═══════════════════╪═══════════════╪═══════════════╪══════════╣");
        System.out.printf( "║ %-17s │ %13s │ %13s │ %8s ║%n",
                "Key Agreement", ecMatch ? "MATCH" : "FAIL", pqcMatch ? "MATCH" : "FAIL", "");
        System.out.println("╚═══════════════════╧═══════════════╧═══════════════╧══════════╝");
    }
}
