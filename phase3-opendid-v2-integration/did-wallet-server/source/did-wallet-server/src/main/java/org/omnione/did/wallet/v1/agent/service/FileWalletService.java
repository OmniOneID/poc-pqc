/*
 * Copyright 2024 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnione.did.wallet.v1.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.base.exception.ErrorCode;
import org.omnione.did.base.exception.OpenDidException;
import org.omnione.did.base.property.WalletProperty;
import org.omnione.did.base.utils.BaseWalletUtil;
import org.omnione.did.wallet.exception.WalletException;
import org.omnione.did.wallet.key.WalletManagerInterface;
import org.omnione.did.wallet.key.data.CryptoKeyPairInfo;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

import java.nio.charset.StandardCharsets;

/**
 * Service for handling file wallet operations.
 * This class provides methods for connecting to a file wallet and generating compact signatures.
 */
@Service
@Slf4j
public class FileWalletService {
    private final WalletProperty walletProperty;
    private final WalletManagerInterface walletManager;


    public FileWalletService(WalletProperty walletProperty) {
        this.walletProperty = walletProperty;
        this.walletManager = BaseWalletUtil.getFileWalletManager();
    }

    /**
     * Connects to the wallet using the wallet file path and password.
     *
     * @throws OpenDidException if the connection to the wallet fails
     */
    public void connectToWallet() {
        try {
            walletManager.connect(walletProperty.getFilePath(), walletProperty.getPassword().toCharArray());
        } catch (WalletException e) {
            log.error("Failed to connect to wallet: {}", e.getMessage());
            throw new OpenDidException(ErrorCode.WALLET_CONNECTION_FAILED);
        }
    }

    /**
     * Generates a compact signature for the given plain text using the key with the given key ID.
     *
     * @param keyId     the key ID of the key to use for signing
     * @param plainText the plain text to sign
     * @return the compact signature as a byte array
     * @throws OpenDidException if the signature generation fails
     */
    public byte[] generateCompactSignature(String keyId, String plainText) {
        return generateCompactSignature(keyId, plainText.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a compact signature for the given plain text using the key with the given key ID.
     *
     * @param keyId     the key ID of the key to use for signing
     * @param plainText the plain text to sign
     * @return the compact signature as a byte array
     * @throws OpenDidException if the signature generation fails
     */
    public byte[] generateCompactSignature(String keyId, byte[] plainText) {
        try {
            if (!walletManager.isConnect()) {
                log.info("Wallet manager disConnect. Connecting to wallet...");
                connectToWallet();
            }

            byte[] signature;
            if ("MlDsa44".equals(walletProperty.getKeyAlgorithm()) && !"keyagree".equals(keyId)) {
                signature = walletManager.generateCompactSignatureFromHash(keyId, plainText);
            } else {
                signature = BaseWalletUtil.generateCompactSignature(walletManager, keyId, plainText);
            }
            log.info("Compact signature generated for keyId: {}", keyId);
            return signature;
        } catch (OpenDidException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenDidException(ErrorCode.WALLET_SIGNATURE_GENERATION_FAILED);
        }
    }

    public WalletManagerInterface initializeWalletWithKeys() {
        // PQC: 설정에서 알고리즘 읽어 keyId별 알고리즘 맵 구성
        CryptoKeyPairInfo.KeyAlgorithmType algorithmType =
                CryptoKeyPairInfo.KeyAlgorithmType.fromValue(walletProperty.getKeyAlgorithm());
        if (algorithmType == null) {
            algorithmType = CryptoKeyPairInfo.KeyAlgorithmType.SECP256r1;
        }
        Map<String, CryptoKeyPairInfo.KeyAlgorithmType> keyAlgorithms = new LinkedHashMap<>();
        keyAlgorithms.put("auth",     algorithmType);
        keyAlgorithms.put("assert",   algorithmType);
        // ECDH key exchange always uses an EC key.
        keyAlgorithms.put("keyagree", CryptoKeyPairInfo.KeyAlgorithmType.SECP256r1);
        keyAlgorithms.put("invoke",   algorithmType);

        return BaseWalletUtil.initializeWalletWithKeys(
                walletProperty.getFilePath(),
                walletProperty.getPassword(),
                keyAlgorithms
        );
    }
}
