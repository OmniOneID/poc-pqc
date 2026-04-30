/*
 * Copyright 2024-2025 OmniOne.
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

package org.omnione.did.sdk.core.api;

import android.content.Context;

import org.omnione.did.sdk.datamodel.did.DidDocVo;
import org.omnione.did.sdk.datamodel.common.enums.RoleType;
import org.omnione.did.sdk.datamodel.common.Proof;
import org.omnione.did.sdk.datamodel.did.DIDDocument;
import org.omnione.did.sdk.datamodel.did.DIDKeyType;
import org.omnione.did.sdk.datamodel.did.VerificationMethod;
import org.omnione.did.sdk.datamodel.util.MessageUtil;
import org.omnione.did.sdk.datamodel.vc.Claim;
import org.omnione.did.sdk.datamodel.vc.VCProof;
import org.omnione.did.sdk.datamodel.vc.VerifiableCredential;
import org.omnione.did.sdk.datamodel.vcschema.VCSchema;
import org.omnione.did.sdk.utility.CryptoUtils;
import org.omnione.did.sdk.utility.DataModels.DigestEnum;
import org.omnione.did.sdk.utility.DigestUtils;
import org.omnione.did.sdk.utility.Encodings.Base16;
import org.omnione.did.sdk.utility.Errors.UtilityException;
import org.omnione.did.sdk.utility.MultibaseUtils;
import org.omnione.did.sdk.wallet.walletservice.db.DBManager;
import org.omnione.did.sdk.wallet.walletservice.db.Preference;
import org.omnione.did.sdk.wallet.walletservice.db.Token;
import org.omnione.did.sdk.wallet.walletservice.db.TokenDao;
import org.omnione.did.sdk.datamodel.token.WalletTokenData;
import org.omnione.did.sdk.datamodel.common.enums.AlgorithmType;
import org.omnione.did.sdk.datamodel.common.enums.WalletTokenPurpose;
import org.omnione.did.sdk.datamodel.token.WalletTokenSeed;
import org.omnione.did.sdk.utility.DataModels.MultibaseType;
import org.omnione.did.sdk.wallet.walletservice.exception.WalletErrorCode;
import org.omnione.did.sdk.wallet.walletservice.exception.WalletException;
import org.omnione.did.sdk.wallet.walletservice.logger.WalletLogger;
import org.omnione.did.sdk.wallet.walletservice.util.WalletUtil;
import org.omnione.did.sdk.core.exception.WalletCoreException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class WalletToken {
    private Context context;
    WalletCore walletCore;
    WalletLogger walletLogger;
    public WalletToken(){}
    public WalletToken(Context context, WalletCore walletCore){
        this.context = context;
        this.walletCore = walletCore;
        walletLogger = WalletLogger.getInstance();
    }

    public void verifyWalletToken(String hWalletToken, List<WalletTokenPurpose.WALLET_TOKEN_PURPOSE> purposeList) throws WalletException{
        DBManager walletDB = DBManager.getDatabases(context);
        TokenDao tokenDao = walletDB.tokenDao();
        if(walletDB.tokenDao().getAll().size() == 0) {
            throw new WalletException(WalletErrorCode.ERR_CODE_WALLET_SELECT_QUERY_FAIL);
        }
        String hWalletToken_db = tokenDao.getAll().get(0).hWalletToken;
        String validUntil_db = tokenDao.getAll().get(0).validUntil;
        String purpose_db = tokenDao.getAll().get(0).purpose;

        walletLogger.d("verifyWalletToken 인가앱 hwallettoken: " + hWalletToken);
        walletLogger.d("verifyWalletToken walletToken db: " + hWalletToken_db);
        walletLogger.d("db: " + validUntil_db + " / " + purpose_db);

        if(!hWalletToken.equals(hWalletToken_db)){
            walletLogger.e("walletToken verification fail");
            throw new WalletException(WalletErrorCode.ERR_CODE_WALLET_VERIFY_TOKEN_FAIL);
        }
        if(!WalletUtil.checkDate(validUntil_db)){
            walletLogger.e("valid until verification fail");
            throw new WalletException(WalletErrorCode.ERR_CODE_WALLET_VERIFY_TOKEN_FAIL);
        }
        boolean isPurpose = false;
        for(WalletTokenPurpose.WALLET_TOKEN_PURPOSE purpose : purposeList) {
            if (purpose_db.equals(purpose.toString())) {
                isPurpose = true;
            }
        }
        if(!isPurpose)
            throw new WalletException(WalletErrorCode.ERR_CODE_WALLET_VERIFY_TOKEN_FAIL);
        walletLogger.d("walletToken verification success");
        walletLogger.d("valid until verification success");
        walletLogger.d("purpose verification success");
    }

    public WalletTokenSeed createWalletTokenSeed(WalletTokenPurpose.WALLET_TOKEN_PURPOSE purpose, String pkgName, String userId) throws UtilityException, WalletException {
        if(purpose == null)
            throw new WalletException(WalletErrorCode.ERR_CODE_WALLET_VERIFY_PARAMETER_FAIL, "purpose");
        if(pkgName.isEmpty())
            throw new WalletException(WalletErrorCode.ERR_CODE_WALLET_VERIFY_PARAMETER_FAIL, "pkgName");
        if(userId.isEmpty())
            throw new WalletException(WalletErrorCode.ERR_CODE_WALLET_VERIFY_PARAMETER_FAIL, "userId");

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                deleteTokenData();
//            }
//        }).start();

        Thread thread = new Thread(() -> deleteTokenData());
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WalletException(WalletErrorCode.ERR_CODE_WALLET_VERIFY_TOKEN_FAIL, "Thread interrupted while deleting token data");
        }

        WalletTokenSeed walletTokenSeed = new WalletTokenSeed();
        walletTokenSeed.setPurpose(purpose);
        walletTokenSeed.setPkgName(pkgName);
        walletTokenSeed.setUserId(userId);
        String nonce = MultibaseUtils.encode(MultibaseType.MULTIBASE_TYPE.BASE_16_UPPER, CryptoUtils.generateNonce(16));
        walletTokenSeed.setNonce(nonce);
        walletTokenSeed.setValidUntil(WalletUtil.createValidUntil(30)); //set valid until

        return walletTokenSeed;
    }

    public String createNonceForWalletToken(String apiGateWayUrl, WalletTokenData walletTokenData) throws UtilityException, WalletException, WalletCoreException, ExecutionException, InterruptedException {
        if(apiGateWayUrl.isEmpty())
            throw new WalletException(WalletErrorCode.ERR_CODE_WALLET_VERIFY_PARAMETER_FAIL, "apiGateWayUrl");
        if(walletTokenData == null)
            throw new WalletException(WalletErrorCode.ERR_CODE_WALLET_VERIFY_PARAMETER_FAIL, "walletTokenData");
        verifyCertVc(RoleType.ROLE_TYPE.APP_PROVIDER, walletTokenData.getProvider().getDID(), walletTokenData.getProvider().getCertVcRef(), apiGateWayUrl);
        if (!verifyWalletTokenDataProof(walletTokenData, apiGateWayUrl))
            throw new WalletException(WalletErrorCode.ERR_CODE_WALLET_CREATE_PROOF_FAIL);

        String resultNonce = MultibaseUtils.encode(MultibaseType.MULTIBASE_TYPE.BASE_16_UPPER, CryptoUtils.generateNonce(16));
        String hWalletToken = createWalletToken(walletTokenData, resultNonce);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Callable<Void> insertTask = () -> {
            insertTokenData(walletTokenData, hWalletToken);
            return null;
        };
        Future<Void> future = executorService.submit(insertTask);
        future.get();
        executorService.shutdown();
        walletLogger.d("resultNonce : " + resultNonce);

        return resultNonce;
    }
    private String createWalletToken(WalletTokenData walletTokenData, String resultNonce) throws UtilityException{
        String walletToken = walletTokenData.toJson() + resultNonce;
        walletToken = Base16.toHex(DigestUtils.getDigest(walletToken.getBytes(), DigestEnum.DIGEST_ENUM.SHA_256));
        walletLogger.d("wallet token : " + walletToken);
        return walletToken;
    }

    private void insertTokenData(WalletTokenData walletTokenData, String hWalletToken){
        DBManager walletDB = DBManager.getDatabases(context);
        TokenDao tokenDao = walletDB.tokenDao();
        Token token = new Token();
        token.idx = WalletUtil.genId();
        token.walletId = Preference.loadWalletId(context);;
        token.validUntil = walletTokenData.getSeed().getValidUntil();
        token.purpose = walletTokenData.getSeed().getPurpose().toString();
        token.pii = walletTokenData.getSha256_pii();
        token.nonce = walletTokenData.getSeed().getNonce();
        token.pkgName = walletTokenData.getSeed().getPkgName();
        token.hWalletToken = hWalletToken;
        token.createDate = WalletUtil.getDate();
        tokenDao.insertAll(token);
    }

    private void deleteTokenData(){
        DBManager walletDB = DBManager.getDatabases(context);
        TokenDao tokenDao = walletDB.tokenDao();
        if(walletDB.tokenDao().getAll().size() != 0) {
            Token token = tokenDao.getAll().get(0);
            tokenDao.deleteByIdx(token.idx);
        }
    }

    private void verifyCertVc(RoleType.ROLE_TYPE roleType, String providerDID, String providerURL, String apiGateWayUrl) throws WalletCoreException, UtilityException, ExecutionException, InterruptedException, WalletException {
        String certVcStr = WalletUtil.getCertVc(providerURL).get();
        VerifiableCredential certVc = MessageUtil.deserialize(certVcStr, VerifiableCredential.class);

        if (!certVc.getCredentialSubject().getId().equals(providerDID))
            throw new WalletException(WalletErrorCode.ERR_CODE_WALLET_DID_MATCH_FAIL);

        String schemaUrl = certVc.getCredentialSchema().getId();
        String schemaData = WalletUtil.getVcSchema(schemaUrl).get();
        VCSchema vcSchema = MessageUtil.deserialize(schemaData, VCSchema.class);
        List<VCSchema.Claim> vcSchemaClaims = vcSchema.getCredentialSubject().getClaims();
        List<Claim> certVcClaims = certVc.getCredentialSubject().getClaims();

        boolean isExistRole = false;
        for (VCSchema.Claim schemaClaim : vcSchemaClaims) {
            for (VCSchema.ClaimDef item : schemaClaim.items) {
                if (item.caption.equals("role")) {
                    for (Claim certVcClaim : certVcClaims) {
                        if (certVcClaim.getCaption().equals(item.caption)) {
                            if (roleType.getValue().equals(certVcClaim.getValue()))
                                isExistRole = true;
                        }
                    }
                }
            }
        }
        if (!isExistRole)
            throw new WalletException(WalletErrorCode.ERR_CODE_WALLET_ROLE_MATCH_FAIL);
        if (!verifyProof(certVcStr, false, apiGateWayUrl))
            throw new WalletException(WalletErrorCode.ERR_CODE_WALLET_VERIFY_CERT_VC_FAIL);
    }

    private boolean verifyProof(String vcStr, boolean isCertVc, String apiGateWayUrl) throws WalletCoreException, UtilityException, WalletException, ExecutionException, InterruptedException {
        VerifiableCredential vc = MessageUtil.deserialize(vcStr, VerifiableCredential.class);
        String did = vc.getIssuer().getId();
        if(isCertVc)
            did = vc.getCredentialSubject().getId();
        String didDoc = WalletUtil.getDidDoc(apiGateWayUrl, did).get();
        DidDocVo didDocVo = MessageUtil.deserialize(didDoc, DidDocVo.class);
        DIDDocument didDocument = MessageUtil.deserialize(new String(MultibaseUtils.decode(didDocVo.getDidDoc())), DIDDocument.class);
        VerificationMethod assertVm = findAssertionVerificationMethod(didDocument);
        if (assertVm == null) {
            throw new WalletException(WalletErrorCode.ERR_CODE_WALLET_VERIFY_CERT_VC_FAIL,
                    "assert verification method not found for did=" + did);
        }
        String encodePubKey = assertVm.getPublicKeyMultibase();
        DIDKeyType.DID_KEY_TYPE keyType = assertVm.getType();
        byte[] signature = MultibaseUtils.decode(vc.getProof().getProofValue());
        VCProof proof = vc.getProof();
        AlgorithmType.ALGORITHM_TYPE algorithmType = resolveAlgorithmType(proof.getType() != null ? proof.getType().toString() : null, keyType, encodePubKey);
        byte[] publicKey = MultibaseUtils.decode(encodePubKey);
        walletLogger.d("verifyProof(certVc) did=" + did
                + " / verificationMethodId=" + assertVm.getId()
                + " / proofType=" + (proof.getType() != null ? proof.getType().toString() : "null")
                + " / keyType=" + (keyType != null ? keyType.toString() : "null")
                + " / algorithmType=" + algorithmType
                + " / publicKeyLength=" + publicKey.length
                + " / signatureLength=" + signature.length);
        proof.setProofValue(null);
        proof.setProofValueList(null);
        vc.setProof(proof);
        String vcJsonForVerify = vc.toJson();
        walletLogger.d("verifyProof(certVc) source length=" + vcJsonForVerify.length()
                + " / sha256=" + sha256Hex(vcJsonForVerify));
        boolean result = walletCore.verify(algorithmType, publicKey, vcJsonForVerify.getBytes(StandardCharsets.UTF_8), signature);
        walletLogger.d("verify certVC : " +result);
        return result;
    }
    private boolean verifyWalletTokenDataProof(WalletTokenData walletTokenData, String apiGateWayUrl) throws WalletCoreException, UtilityException, WalletException, ExecutionException, InterruptedException {
        String did = walletTokenData.getProvider().getDID();
        String didDoc = WalletUtil.getDidDoc(apiGateWayUrl, did).get();
        DidDocVo didDocVo = MessageUtil.deserialize(didDoc, DidDocVo.class);
        DIDDocument didDocument = MessageUtil.deserialize(new String(MultibaseUtils.decode(didDocVo.getDidDoc())), DIDDocument.class);
        VerificationMethod assertVm = findAssertionVerificationMethod(didDocument);
        if (assertVm == null) {
            throw new WalletException(WalletErrorCode.ERR_CODE_WALLET_CREATE_PROOF_FAIL,
                    "assert verification method not found for did=" + did);
        }
        String encodePubKey = assertVm.getPublicKeyMultibase();
        DIDKeyType.DID_KEY_TYPE keyType = assertVm.getType();
        byte[] signature = MultibaseUtils.decode(walletTokenData.getProof().getProofValue());
        Proof proof = walletTokenData.getProof();
        AlgorithmType.ALGORITHM_TYPE algorithmType = resolveAlgorithmType(proof.getType() != null ? proof.getType().toString() : null, keyType, encodePubKey);
        byte[] publicKey = MultibaseUtils.decode(encodePubKey);
        walletLogger.d("verifyWalletTokenDataProof did=" + did
                + " / verificationMethodId=" + assertVm.getId()
                + " / proofType=" + (proof.getType() != null ? proof.getType().toString() : "null")
                + " / keyType=" + (keyType != null ? keyType.toString() : "null")
                + " / algorithmType=" + algorithmType
                + " / publicKeyLength=" + publicKey.length
                + " / signatureLength=" + signature.length);
        proof.setProofValue(null);
        walletTokenData.setProof(proof);
        String verifySource = walletTokenData.toJson();
        walletLogger.d("verifyWalletTokenDataProof source length=" + verifySource.length()
                + " / sha256=" + sha256Hex(verifySource)
                + " / source=" + verifySource);
        boolean result = walletCore.verify(algorithmType, publicKey, verifySource.getBytes(StandardCharsets.UTF_8), signature);
        return result;
    }

    private VerificationMethod findAssertionVerificationMethod(DIDDocument didDocument) {
        if (didDocument == null || didDocument.getVerificationMethod() == null) {
            walletLogger.d("findAssertionVerificationMethod verificationMethods is empty");
            return null;
        }
        for (VerificationMethod vm : didDocument.getVerificationMethod()) {
            walletLogger.d("verificationMethod candidate"
                    + " / id=" + vm.getId()
                    + " / type=" + (vm.getType() != null ? vm.getType().toString() : "null"));
            if (isAssertionVerificationMethod(vm.getId())) {
                return vm;
            }
        }
        return null;
    }

    private boolean isAssertionVerificationMethod(String verificationMethodId) {
        return verificationMethodId != null
                && ("assert".equals(verificationMethodId)
                || verificationMethodId.endsWith("#assert"));
    }

    private AlgorithmType.ALGORITHM_TYPE resolveAlgorithmType(String proofType,
                                                              DIDKeyType.DID_KEY_TYPE keyType,
                                                              String encodePubKey) throws UtilityException {
        if ("MlDsa44Signature2024".equals(proofType)) {
            return AlgorithmType.ALGORITHM_TYPE.ML_DSA_44;
        }
        if ("Secp256r1Signature2018".equals(proofType)) {
            return AlgorithmType.ALGORITHM_TYPE.SECP256R1;
        }
        if (keyType != null) {
            return DIDKeyType.DID_KEY_TYPE.convertTo(keyType);
        }
        if (encodePubKey != null && !encodePubKey.isEmpty() && MultibaseUtils.decode(encodePubKey).length != 33) {
            return AlgorithmType.ALGORITHM_TYPE.ML_DSA_44;
        }
        return AlgorithmType.ALGORITHM_TYPE.SECP256R1;
    }

    private String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "sha256-error:" + e.getMessage();
        }
    }
}
