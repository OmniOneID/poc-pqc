package org.omnione.did.poc.pqc.wallet;

import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.omnione.did.crypto.enums.EccCurveType;
import org.omnione.did.crypto.util.CryptoUtils;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.wallet.enums.WalletEncryptType;
import org.omnione.did.wallet.key.WalletManagerFactory;
import org.omnione.did.wallet.key.WalletManagerFactory.WalletManagerType;
import org.omnione.did.wallet.key.WalletManagerInterface;
import org.omnione.did.wallet.key.data.CryptoKeyPairInfo.KeyAlgorithmType;

/**
 * Wallet SDK로 Secp256r1 키 생성/서명/조회.
 */
public class EcWalletKeyProvider {

    private static final String ALGORITHM = "EC";
    private static final String PROVIDER = "BC";

    private final WalletManagerInterface wallet;
    private final String keyId;

    static {
        if (Security.getProvider(PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public EcWalletKeyProvider(String keyId, char[] password) throws Exception {
        this.keyId = keyId;
        this.wallet = WalletManagerFactory.getWalletManager(WalletManagerType.FILE);

        String path = Files.createTempDirectory("ec-wallet")
                .resolve("issuer.wallet").toString();

        wallet.create(path, password, WalletEncryptType.AES_256_CBC_PKCS5Padding);
        wallet.connect(path, password);

        wallet.generateRandomKey(keyId, KeyAlgorithmType.SECP256r1);
    }

    public PublicKey getPublicKey() throws Exception {
        String multibase = wallet.getPublicKey(keyId);
        byte[] compressedBytes = MultiBaseUtils.decode(multibase);
        // Wallet SDK는 EC 공개키를 압축 형태로 저장 → 비압축 후 X509으로 변환
        byte[] uncompressedBytes = CryptoUtils.unCompressPublicKey(compressedBytes, EccCurveType.Secp256r1);
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM, PROVIDER);
        return kf.generatePublic(new X509EncodedKeySpec(uncompressedBytes));
    }

    public String getPublicKeyMultibase() throws Exception {
        return wallet.getPublicKey(keyId);
    }

    public byte[] sign(byte[] data) throws Exception {
        // EC 서명은 SHA-256 해시 후 compact signature 생성 (ML-DSA와 달리 외부 해싱 필요)
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
        return wallet.generateCompactSignatureFromHash(keyId, hash);
    }

    public String getKeyId() {
        return keyId;
    }

    public void disconnect() {
        wallet.disConnect();
    }
}
