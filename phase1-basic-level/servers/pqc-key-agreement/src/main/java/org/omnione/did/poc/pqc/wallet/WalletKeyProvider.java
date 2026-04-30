package org.omnione.did.poc.pqc.wallet;

import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.wallet.enums.WalletEncryptType;
import org.omnione.did.wallet.key.WalletManagerFactory;
import org.omnione.did.wallet.key.WalletManagerFactory.WalletManagerType;
import org.omnione.did.wallet.key.WalletManagerInterface;
import org.omnione.did.wallet.key.data.CryptoKeyPairInfo.KeyAlgorithmType;

// wallet sdk로 ML-DSA-44 키 생성/서명/조회
public class WalletKeyProvider {

    private static final String ALGORITHM = "ML-DSA-44";
    private static final String PROVIDER = "BC";

    private final WalletManagerInterface wallet;
    private final String keyId;

    static {
        if (Security.getProvider(PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    // wallet 생성 + ML-DSA-44 키 생성
    public WalletKeyProvider(String keyId, char[] password) throws Exception {
        this.keyId = keyId;
        this.wallet = WalletManagerFactory.getWalletManager(WalletManagerType.FILE);

        String path =  Files.createTempDirectory("pqc-wallet")
                    .resolve("issuer.wallet").toString();

        wallet.create(path, password, WalletEncryptType.AES_256_CBC_PKCS5Padding);
        wallet.connect(path, password);

        wallet.generateRandomKey(keyId, KeyAlgorithmType.ML_DSA_44);
    }

    // 공개키 조회
    public PublicKey getPublicKey() throws Exception {
        String multibase = wallet.getPublicKey(keyId);
        byte[] pubKeyBytes = MultiBaseUtils.decode(multibase);
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM, PROVIDER);
        return kf.generatePublic(new X509EncodedKeySpec(pubKeyBytes));
    }

    // multibase 공개키
    public String getPublicKeyMultibase() throws Exception {
        return wallet.getPublicKey(keyId);
    }

    // 서명
    public byte[] sign(byte[] data) throws Exception {
        return wallet.generateCompactSignatureFromHash(keyId, data);
    }

    // 검증
    public boolean verify(byte[] data, byte[] signature) throws Exception {
        PublicKey publicKey = getPublicKey();
        java.security.Signature verifier = java.security.Signature.getInstance(ALGORITHM, PROVIDER);
        verifier.initVerify(publicKey);
        verifier.update(data);
        return verifier.verify(signature);
    }

    public String getKeyId() {
        return keyId;
    }

    // 연결 해제
    public void disconnect() {
        wallet.disConnect();
    }
}
