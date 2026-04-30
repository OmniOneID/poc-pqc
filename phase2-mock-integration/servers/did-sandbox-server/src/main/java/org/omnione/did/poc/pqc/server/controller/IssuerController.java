/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.controller;

import com.google.gson.Gson;
import org.omnione.did.poc.pqc.server.ResponseMessage;
import org.omnione.did.poc.pqc.server.service.MockService;
import org.omnione.did.poc.pqc.server.util.JsonUtils;
import org.omnione.did.poc.pqc.server.util.MockDataFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import lombok.Generated;
import org.omnione.did.crypto.engines.CipherInfo;
import org.omnione.did.crypto.enums.EccCurveType;
import org.omnione.did.crypto.enums.MultiBaseType;
import org.omnione.did.crypto.enums.SymmetricCipherType;
import org.omnione.did.crypto.enums.SymmetricPaddingType;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.util.CryptoUtils;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.data.model.schema.VcSchema;
import org.omnione.did.data.model.vc.VerifiableCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/issuer/api/v1"})
public class IssuerController {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(IssuerController.class);
    private final MockService mockService;

    public IssuerController(MockService mockService) {
        this.mockService = mockService;
    }

    @PostMapping(value={"/request-register-wallet"})
    public String registerWallet(@RequestBody String request) {
        try {
            return ResponseMessage.Tas.RESPONSE_REGISTER_WALLET;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping(value={"/vc/vcschema"})
    public String getVcSchema(@RequestParam(value="name") String name) {
        VcSchema schema = MockDataFactory.createStudentIdVcSchema();
        return schema.toJson();
    }

    @GetMapping(value={"/certificate-vc"})
    public String getCertVc() {
        String response2 = this.getCerificateVC();
        return response2;
    }

    public String getCerificateVC() {
        VerifiableCredential vc = MockDataFactory.createCertificateVc(
                "did:omn:issuer",    // subjectDid
                "o=issuer",          // subjectValue
                "Issuer",            // roleValue
                "did:omn:tas",       // issuerDid
                "tas",               // issuerName
                "did:omn:tas?versionId=1#assert"  // proofVerificationMethod
        );
        org.omnione.did.data.model.vc.VcProof proof = (org.omnione.did.data.model.vc.VcProof) vc.getProof();
        proof.setProofValueList(this.mockService.generateProofValueList(vc));
        return this.mockService.signVc(vc);
    }

    @PostMapping(value={"/inspect-propose-issue"})
    public String inspectIssueVc(@RequestBody String request) {
        try {
            HashMap<String, String> responseMap = new HashMap<String, String>();
            responseMap.put("txId", ResponseMessage.Tas.RESPONSE_PROPOSE_REGISTER_USER);
            responseMap.put("refId", "TNQMxiWPVTuFpAjWl");
            Gson gson = JsonUtils.GSON;
            return gson.toJson(responseMap);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] decryptEncReqVc(String encReqVc, byte[] iv, byte[] clientPublicKey) {
        try {
            byte[] decryptedReqVc = MultiBaseUtils.decode(encReqVc);
            SymmetricPaddingType symmetricPaddingType = SymmetricPaddingType.PKCS5;
            SymmetricCipherType symmetricCipherType = SymmetricCipherType.AES_256_CBC;
            // ECDH는 항상 ECC 키 사용
            byte[] sharedSecret = IssuerController.generateSharedSecret(clientPublicKey, MultiBaseUtils.decode(ResponseMessage.Crypto.ECC_PRIVATE_KEY));
            byte[] mergedSharedSecret = IssuerController.mergeSharedSecretAndNonce(sharedSecret, MultiBaseUtils.decode(ResponseMessage.Crypto.E2E_NONCE_VC));
            ResponseMessage.Session.SESSION_KEY_VC = MultiBaseUtils.encode(mergedSharedSecret, MultiBaseType.base64);
            byte[] sessionKey = MultiBaseUtils.decode(ResponseMessage.Session.SESSION_KEY_VC);
            CipherInfo cipherInfo = new CipherInfo(symmetricCipherType, symmetricPaddingType);
            return CryptoUtils.decrypt(decryptedReqVc, cipherInfo, sessionKey, iv);
        }
        catch (Exception e) {
            log.error("\t--> Exception occurred during decryptEncReqVc: {}", (Object)e.getMessage(), (Object)e);
            throw new RuntimeException(e);
        }
    }

    private byte[] decryptEncReqVcMlKem(String encReqVc, byte[] iv, String ciphertext) {
        try {
            byte[] decryptedReqVc = MultiBaseUtils.decode(encReqVc);
            SymmetricPaddingType symmetricPaddingType = SymmetricPaddingType.PKCS5;
            SymmetricCipherType symmetricCipherType = SymmetricCipherType.AES_256_CBC;
            // ML-KEM: decapsulate로 shared secret 획득
            byte[] sharedSecret = org.omnione.did.poc.pqc.server.util.PqcCryptoUtils.mlKemDecapsulate(
                    ResponseMessage.Crypto.MLKEM_DECAP_KEY, ciphertext);
            byte[] mergedSharedSecret = IssuerController.mergeSharedSecretAndNonce(sharedSecret, MultiBaseUtils.decode(ResponseMessage.Crypto.E2E_NONCE_VC));
            ResponseMessage.Session.SESSION_KEY_VC = MultiBaseUtils.encode(mergedSharedSecret, MultiBaseType.base64);
            byte[] sessionKey = MultiBaseUtils.decode(ResponseMessage.Session.SESSION_KEY_VC);
            CipherInfo cipherInfo = new CipherInfo(symmetricCipherType, symmetricPaddingType);
            return CryptoUtils.decrypt(decryptedReqVc, cipherInfo, sessionKey, iv);
        }
        catch (Exception e) {
            log.error("\t--> Exception occurred during decryptEncReqVcMlKem: {}", (Object)e.getMessage(), (Object)e);
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/complete-vc"})
    public String confirmIssueVc(@RequestBody String request) {
        try {
            return ResponseMessage.Tas.RESPONSE_PROPOSE_REGISTER_USER;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] generateSharedSecret(byte[] publicKey, byte[] privateKey) {
        try {
            return CryptoUtils.generateSharedSecret(publicKey, privateKey, EccCurveType.Secp256r1);
        }
        catch (CryptoException e) {
            log.error("Failed to generate shared secret: {}", (Object)e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static byte[] mergeSharedSecretAndNonce(byte[] sharedSecret, byte[] nonce) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(sharedSecret, 0, sharedSecret.length);
            digest.update(nonce, 0, nonce.length);
            byte[] combinedResult = digest.digest();
            return Arrays.copyOfRange(combinedResult, 0, 32);
        }
        catch (NoSuchAlgorithmException e) {
            log.error("Failed to merge shared secret and nonce: {}", (Object)e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private byte[] encryptServerTokenData(String stdJson, byte[] iv, String sKey) {
        try {
            SymmetricPaddingType symmetricPaddingType = SymmetricPaddingType.PKCS5;
            SymmetricCipherType symmetricCipherType = SymmetricCipherType.AES_256_CBC;
            byte[] sessionKey = MultiBaseUtils.decode(sKey);
            CipherInfo cipherInfo = new CipherInfo(symmetricCipherType, symmetricPaddingType);
            return CryptoUtils.encrypt(stdJson.getBytes(StandardCharsets.UTF_8), cipherInfo, sessionKey, iv);
        }
        catch (Exception e) {
            log.error("\t--> Exception occurred during encryptServerTokenData: {}", (Object)e.getMessage(), (Object)e);
            throw new RuntimeException(e);
        }
    }

}
