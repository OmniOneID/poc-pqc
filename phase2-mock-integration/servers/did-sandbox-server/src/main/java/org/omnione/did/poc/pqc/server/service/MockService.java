/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.omnione.did.poc.pqc.server.ResponseMessage;
import org.omnione.did.poc.pqc.server.datamodel.data.KeyPairInfo;
import org.omnione.did.poc.pqc.server.util.JsonUtils;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.omnione.did.crypto.enums.DigestType;
import org.omnione.did.crypto.enums.EccCurveType;
import org.omnione.did.crypto.enums.MultiBaseType;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.util.CryptoUtils;
import org.omnione.did.crypto.util.DigestUtils;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.crypto.util.SignatureUtils;
import org.omnione.did.wallet.crypto.encryption.EncryptionHelper;
import org.omnione.did.wallet.exception.WalletErrorCode;
import org.omnione.did.wallet.exception.WalletException;
import org.omnione.did.wallet.key.adapter.WalletManagerAdapter;
import org.omnione.did.data.model.did.Proof;
import org.omnione.did.data.model.vc.Claim;
import org.omnione.did.data.model.vc.CredentialSubject;
import org.omnione.did.data.model.vc.VerifiableCredential;
import org.omnione.did.data.model.vc.VcProof;
import org.omnione.did.data.model.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MockService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(MockService.class);
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public MockService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String addProof(String msg) {
        String result = "";
        try {
            if (org.omnione.did.poc.pqc.server.config.PqcConfig.isMlDsa44()) {
                // ML-DSA-44: 원본 데이터를 직접 서명 (해싱 없음, compact 변환 없음)
                result = org.omnione.did.poc.pqc.server.util.PqcCryptoUtils.sign(
                        ResponseMessage.Crypto.PRIVATE_KEY,
                        msg.getBytes(StandardCharsets.UTF_8));
            } else {
                // 기존 ECC: SHA-256 해시 후 compact 서명
                result = this.signData(msg);
            }
        }
        catch (CryptoException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * ECC(Secp256r1) 전용 서명. PqcConfig 설정과 무관하게 항상 ECC로 서명한다.
     * ECDH proof 등 ECC가 필수인 곳에서 사용.
     */
    public String addProofEcc(String msg) {
        try {
            return this.signData(msg);
        } catch (CryptoException e) {
            throw new RuntimeException(e);
        }
    }

    public String signData(String signatureMessage) throws CryptoException {
        byte[] signature = this.generateCompactSignature(signatureMessage);
        return MultiBaseUtils.encode(signature, MultiBaseType.base64);
    }

    public byte[] generateCompactSignature(String plainText) {
        return this.generateCompactSignature(plainText.getBytes(StandardCharsets.UTF_8));
    }

    public byte[] generateCompactSignature(byte[] plainText) {
        try {
            byte[] hashedSource = DigestUtils.getDigest(plainText, DigestType.SHA256);
            EncryptionHelper encryptionHelper = new EncryptionHelper();
            // ECC 전용 서명 — 항상 ECC 키 사용
            byte[] compressedPublicKeyBytes = this.getUncompressPublicKey(ResponseMessage.Crypto.ECC_PUBLIC_KEY);
            WalletManagerAdapter walletManagerAdapter = new WalletManagerAdapter();
            KeyPairInfo keyPairInfo = null;
            PrivateKey privateKey = encryptionHelper.getPrivateKeyObject(MultiBaseUtils.decode(ResponseMessage.Crypto.ECC_PRIVATE_KEY));
            PublicKey publicKey = encryptionHelper.getPublicKeyObject(compressedPublicKeyBytes);
            keyPairInfo = new KeyPairInfo("Secp256r1", privateKey, publicKey);
            byte[] signature = this.compactSignatureFromHashedData(keyPairInfo, hashedSource);
            return signature;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getUncompressPublicKey(String mEncodedCompressedKey) throws WalletException {
        try {
            byte[] uncompressedPublicKeyBytes = CryptoUtils.unCompressPublicKey(MultiBaseUtils.decode(mEncodedCompressedKey), EccCurveType.Secp256r1);
            return uncompressedPublicKeyBytes;
        }
        catch (CryptoException var4) {
            throw new WalletException(WalletErrorCode.ERR_CODE_CRYPTO_UNCOMPRESS_PUBLIC_KEY_FAIL, (Throwable)var4);
        }
    }

    private byte[] compactSignatureFromHashedData(KeyPairInfo keyPairInfo, byte[] hashedSource) throws WalletException {
        byte[] signature = this.sign(hashedSource, (ECPrivateKey)keyPairInfo.getKeyPair().getPrivateKey());
        byte[] compactSignature = this.getCompactSignature(keyPairInfo.getAlgorithm(), signature, keyPairInfo.getKeyPair().getPublicKey(), hashedSource);
        return compactSignature;
    }

    public byte[] sign(byte[] originalHashedMessage, PrivateKey privateKey) {
        try {
            return SignatureUtils.generateEccSignatureFromHashedData(privateKey, originalHashedMessage);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getCompactSignature(String algorithm, byte[] signedData, PublicKey publicKey, byte[] originalHashedMessage) throws WalletException {
        try {
            return SignatureUtils.convertToCompactSignature(publicKey, originalHashedMessage, signedData, EccCurveType.Secp256r1);
        }
        catch (CryptoException e) {
            throw new WalletException(WalletErrorCode.ERR_CODE_SIG_COMPRESS_SIGN_FAIL, (Throwable)e);
        }
    }

    public Map<String, Object> signMap(Map<String, Object> map) {
        try {
            String serializedJson = JsonUtils.serializeAndSort(map);
            String proofValue = this.addProof(serializedJson);
            HashMap<String, String> proof = (HashMap<String, String>)map.get("proof");
            if (proof == null) {
                proof = new HashMap<String, String>();
                map.put("proof", proof);
            }
            proof.put("proofValue", proofValue);
            return map;
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to sign map", e);
        }
    }

    public String signObjectAndReturnJson(Object obj) {
        try {
            String serializedJson = JsonUtils.serializeAndSort(obj);
            String proofValue = this.addProof(serializedJson);
            Map<String, Object> map = this.objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>(){});
            HashMap<String, String> proof = (HashMap<String, String>)map.get("proof");
            if (proof == null) {
                proof = new HashMap<String, String>();
                map.put("proof", proof);
            }
            proof.put("proofValue", proofValue);
            return this.objectMapper.writeValueAsString(map);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to sign object", e);
        }
    }

    /**
     * VerifiableCredential SDK 모델 객체를 서명하고 JSON 반환.
     * 클라이언트 검증(verifyProof)과 동일한 방식으로 직렬화:
     * proof.proofValue=null, proof.proofValueList=null 상태에서 vc.toJson()
     */
    public String signVc(VerifiableCredential vc) {
        try {
            VcProof proof = (VcProof) vc.getProof();
            String savedProofValue = proof.getProofValue();
            List<String> savedProofValueList = proof.getProofValueList();

            proof.setProofValue(null);
            proof.setProofValueList(null);
            vc.setProof(proof);

            String json = vc.toJson();
            String proofValue = this.addProof(json);

            proof.setProofValue(proofValue);
            proof.setProofValueList(savedProofValueList);
            vc.setProof(proof);

            return vc.toJson();
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign VC", e);
        }
    }

    /**
     * Proof를 포함하는 임의의 Map 응답을 서명하고 JSON 반환.
     * SDK 모델과 Map이 혼재할 때 사용.
     */
    public String signMapAndReturnJson(Map<String, Object> map) {
        try {
            Map<String, Object> signedMap = this.signMap(map);
            return this.objectMapper.writeValueAsString(signedMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign map", e);
        }
    }

    /**
     * VC의 각 claim에 대해 개별 서명을 생성하여 proofValueList를 반환한다.
     * 클라이언트 검증과 동일한 방식: proof.proofValue=null, proofValueList=null + vc.toJson()
     */
    public List<String> generateProofValueList(VerifiableCredential originalVc) {
        List<Claim> claims = originalVc.getCredentialSubject().getClaims();
        List<String> proofValueList = new ArrayList<>();

        for (Claim claim : claims) {
            VerifiableCredential tmpVc = new VerifiableCredential();
            tmpVc.fromJson(originalVc.toJson());

            tmpVc.getCredentialSubject().setClaims(Collections.singletonList(claim));

            VcProof tmpProof = (VcProof) tmpVc.getProof();
            tmpProof.setProofValue(null);
            tmpProof.setProofValueList(null);
            tmpVc.setProof(tmpProof);

            String serializedJson = tmpVc.toJson();
            proofValueList.add(this.addProof(serializedJson));
        }

        return proofValueList;
    }
}
