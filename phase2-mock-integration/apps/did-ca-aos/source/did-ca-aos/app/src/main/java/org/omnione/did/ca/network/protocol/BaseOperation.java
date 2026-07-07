package org.omnione.did.ca.network.protocol;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.omnione.did.ca.config.Config;
import org.omnione.did.ca.config.Preference;
import org.omnione.did.ca.logger.CaLog;
import org.omnione.did.ca.network.HttpUrlConnection;
import org.omnione.did.ca.network.protocol.token.CreateTokenReqVO;
import org.omnione.did.ca.util.CaUtil;
import org.omnione.did.ca.util.TokenUtil;
import org.omnione.did.sdk.communication.exception.CommunicationException;
import org.omnione.did.sdk.core.api.WalletApi;
import org.omnione.did.sdk.core.exception.WalletCoreException;
import org.omnione.did.sdk.datamodel.common.enums.AlgorithmType;
import org.omnione.did.sdk.datamodel.common.enums.EllipticCurveType;
import org.omnione.did.sdk.datamodel.common.enums.ServerTokenPurpose;
import org.omnione.did.sdk.datamodel.common.enums.SymmetricCipherType;
import org.omnione.did.sdk.datamodel.common.enums.SymmetricPaddingType;
import org.omnione.did.sdk.datamodel.common.enums.WalletTokenPurpose;
import org.omnione.did.sdk.datamodel.protocol.P132RequestVo;
import org.omnione.did.sdk.datamodel.security.AccMlKem;
import org.omnione.did.sdk.datamodel.security.ReqEcdh;
import org.omnione.did.sdk.datamodel.security.ReqMlKem;
import org.omnione.did.sdk.core.keymanager.supportalgorithm.MlKem768Manager;
import org.omnione.did.sdk.datamodel.token.AttestedAppInfo;
import org.omnione.did.sdk.datamodel.token.ServerTokenSeed;
import org.omnione.did.sdk.datamodel.token.SignedWalletInfo;
import org.omnione.did.sdk.datamodel.token.WalletTokenSeed;
import org.omnione.did.sdk.datamodel.util.MessageUtil;
import org.omnione.did.sdk.utility.CryptoUtils;
import org.omnione.did.sdk.utility.DataModels.EcType;
import org.omnione.did.sdk.utility.DataModels.MultibaseType;
import org.omnione.did.sdk.utility.Errors.UtilityException;
import org.omnione.did.sdk.utility.MultibaseUtils;
import org.omnione.did.sdk.wallet.walletservice.exception.WalletException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
public abstract class BaseOperation {

    protected final Context context;
    private OperationType operationType;
    private final Object lockObject = new Object();
    protected Map<String, String> requestDataMap;
    private Callback callback;

    public enum OperationType {
        USER_REGISTRATION,
        USER_UPDATE,
        VC_ISSUERANCE,
        VP_SUBMISSION
    }
    BaseOperation(Context context, OperationType operationType, Map<String, String> requestData) {
        this.context = context;
        this.operationType = operationType;
        this.requestDataMap = requestData;
    }

    public interface Callback {
        void onSuccess(ProtocolData resultData);

        void onFailure(int errorCode, String errorMessage);
    }

    public static BaseOperation getInstance(Context context, OperationType operationType, Map<String, String> requestData) {
        switch (operationType) {
            case OperationType.USER_REGISTRATION:
                return new UserRegistration(context, requestData);
            case OperationType.VP_SUBMISSION:
                return new VpSubmission(context, requestData);
            case OperationType.USER_UPDATE:
                return new UserDidUpdate(context, requestData);
            case OperationType.VC_ISSUERANCE:
                return new VcIssuance(context, requestData);
            default:
                throw new IllegalArgumentException("Operation type cannot be null");
        }
    }

    public abstract void preExecute();

    public abstract void execute();

    public BaseOperation setCallback(@NonNull Callback callback) {
        this.callback = callback;
        return this;
    }

    protected void cancel() {
        synchronized (lockObject) {
            callback = null;
        }
    }

    protected void finish(ProtocolData resultData) {
        synchronized (lockObject) {
            if (callback != null) {
                callback.onSuccess(resultData);
            }
        }
    }

    protected void onError(Throwable throwable) {

        CaLog.d("finally onError :"+throwable);

        Throwable cause = throwable;
        if (throwable instanceof java.util.concurrent.CompletionException && throwable.getCause() != null) {
            cause = throwable.getCause();
        }

        final Throwable finalCause = cause;

        synchronized (lockObject) {
            if (callback != null) {
                if (finalCause instanceof WalletCoreException e) {
                    callback.onFailure(e.getErrorCode(), e.getMessage());
                } else if (finalCause instanceof UtilityException e) {
                    callback.onFailure(e.getErrorCode(), e.getMessage());
                } else if (finalCause instanceof WalletException e) {
                    callback.onFailure(e.getErrorCode(), e.getMessage());
                } else if (finalCause instanceof CommunicationException e) {
                    CaLog.d("CommunicationException errorCode :"+e.getErrorCode());
                    callback.onFailure(e.getErrorCode(), e.getMessage());
                } else {
                    Exception e = (Exception) finalCause;
                    callback.onFailure(-1, e.getMessage());
                }
            }
        }
    }

    /**
     * Key agreement 수행: Config에 따라 ECDH 또는 ML-KEM 선택
     */
    protected CompletableFuture<ProtocolData> requestKeyAgreement(ProtocolData protocolData) {
        if (org.omnione.did.sdk.wallet.walletservice.config.Config.isMlKemKeyAgreement()) {
            return requestMLKEM(protocolData);
        } else {
            return requestECDH(protocolData);
        }
    }

    protected CompletableFuture<ProtocolData> requestECDH(ProtocolData protocolData) {
        CaLog.d("requestECDH");
        CompletableFuture<ProtocolData> completableFuture = new CompletableFuture<>();
        try {
            P132RequestVo requestVo = new P132RequestVo(CaUtil.createMessageId(context), protocolData.getTxId());
            ReqEcdh reqEcdh = new ReqEcdh();
            WalletApi walletApi = WalletApi.getInstance(context);
            CaLog.d("use ecdh holder key");
            String did = walletApi.getDIDDocument(2).getId();
            reqEcdh.setClient(did);
            protocolData.setClientNonce(CryptoUtils.generateNonce(16));
            protocolData.setDhKeyPair(CryptoUtils.generateECKeyPair(EcType.EC_TYPE.SECP256_R1));
            reqEcdh.setClientNonce(MultibaseUtils.encode(MultibaseType.MULTIBASE_TYPE.BASE_58_BTC, protocolData.getClientNonce()));
            reqEcdh.setCurve(EllipticCurveType.ELLIPTIC_CURVE_TYPE.SECP256R1);
            reqEcdh.setPublicKey(protocolData.getDhKeyPair().getPublicKey());
            reqEcdh.setCandidate(new ReqEcdh.Ciphers(List.of(SymmetricCipherType.SYMMETRIC_CIPHER_TYPE.AES256CBC)));
            reqEcdh = (ReqEcdh) walletApi.addProofsToDocument(reqEcdh, List.of("keyagree"), did, 2, null, false);
            requestVo.setReqEcdh(reqEcdh);

            String result = new HttpUrlConnection().send(context, org.omnione.did.ca.config.Config.TAS.REQUEST_ECDH, "POST", requestVo.toJson());
            protocolData.setEcdhResult(result);
            CaLog.d("ECDH success : " + protocolData.getEcdhResult());

            completableFuture.complete(protocolData);

        } catch (WalletException | UtilityException | WalletCoreException e) {
            CaLog.d("ECDH ex : " + e.getMessage());
            throw new CompletionException(e);
        }

        return completableFuture;
    }

    protected CompletableFuture<ProtocolData> requestMLKEM(ProtocolData protocolData) {
        // Option 1 (Wallet-as-receiver):
        //   1) Wallet generates ephemeral ML-KEM-768 keypair
        //   2) Sends own publicKey in reqMlKem (no pre-lookup of TAS DID Doc)
        //   3) Server performs encap and returns ciphertext in accMlKem
        //   4) Wallet decaps with ephemeral sk to recover sharedSecret
        CaLog.d("requestMLKEM (Option 1)");
        CompletableFuture<ProtocolData> completableFuture = new CompletableFuture<>();
        try {
            WalletApi walletApi = WalletApi.getInstance(context);
            String did = walletApi.getDIDDocument(2).getId();

            MlKem768Manager kemManager = new MlKem768Manager();
            org.omnione.did.sdk.core.keymanager.datamodel.KeyGenerationInfo ephPair = kemManager.generateKey();
            byte[] ephemeralSk = MultibaseUtils.decode(ephPair.getPrivateKey()); // PKCS8 bytes
            String ephPublicKeyMultibase = ephPair.getPublicKey();

            ReqMlKem reqMlKem = new ReqMlKem();
            reqMlKem.setClient(did);
            protocolData.setClientNonce(CryptoUtils.generateNonce(16));
            reqMlKem.setClientNonce(MultibaseUtils.encode(MultibaseType.MULTIBASE_TYPE.BASE_58_BTC, protocolData.getClientNonce()));
            reqMlKem.setAlgorithm("ML-KEM-768");
            reqMlKem.setPublicKey(ephPublicKeyMultibase);
            reqMlKem.setCipher(SymmetricCipherType.SYMMETRIC_CIPHER_TYPE.AES256CBC);
            reqMlKem.setPadding(SymmetricPaddingType.SYMMETRIC_PADDING_TYPE.PKCS5);
            reqMlKem = (ReqMlKem) walletApi.addProofsToDocument(reqMlKem, List.of("keyagree"), did, 2, null, false);

            P132RequestVo requestVo = new P132RequestVo(CaUtil.createMessageId(context), protocolData.getTxId());
            requestVo.setReqMlKem(reqMlKem);

            String result = new HttpUrlConnection().send(context, org.omnione.did.ca.config.Config.TAS.REQUEST_ML_KEM, "POST", requestVo.toJson());

            // Parse server response and decap ciphertext to recover sharedSecret
            org.omnione.did.sdk.datamodel.protocol.P132ResponseVo respVo =
                    MessageUtil.deserialize(result, org.omnione.did.sdk.datamodel.protocol.P132ResponseVo.class);
            AccMlKem accMlKem = respVo.getAccMlKem();
            if (accMlKem == null || accMlKem.getCiphertext() == null) {
                throw new IllegalStateException("Option 1 ML-KEM response missing accMlKem.ciphertext");
            }
            byte[] ctBytes = MultibaseUtils.decode(accMlKem.getCiphertext());
            byte[] sharedSecret = kemManager.decapsulate(ephemeralSk, ctBytes);

            protocolData.setMlKemSharedSecret(sharedSecret);
            protocolData.setMlKemResult(result);
            protocolData.setEcdhResult(result);

            CaLog.d("ML-KEM success (Option 1)");
            completableFuture.complete(protocolData);

        } catch (WalletException | UtilityException | WalletCoreException e) {
            CaLog.d("ML-KEM ex : " + e.getMessage());
            throw new CompletionException(e);
        }
        // Note: MultibaseUtils.decode(...) throws UtilityException, already in the
        // multi-catch above. No additional handling needed.

        return completableFuture;
    }

    protected CompletableFuture<ProtocolData> requestWalletTokenData(ProtocolData protocolData) {
        CaLog.d("requestWalletTokenData");
        CompletableFuture<ProtocolData> completableFuture = new CompletableFuture<>();
        try {
            WalletApi walletApi = WalletApi.getInstance(context);
            WalletTokenSeed walletTokenSeed = walletApi.createWalletTokenSeed(WalletTokenPurpose.WALLET_TOKEN_PURPOSE.UPDATE_DID,
                    CaUtil.getPackageName(context),
                    Preference.getUserIdForDemo(context));
            String result = new HttpUrlConnection().send(context, Config.CAS.REQUEST_WALLET_TOKENDATA, "POST", walletTokenSeed.toJson());
            String token = TokenUtil.createHashWalletToken(result, context);
            protocolData.sethWalletToken(token);
            CaLog.d("requestCreateToken success hWalletToken: " + protocolData.gethWalletToken());

            completableFuture.complete(protocolData);

        } catch (UtilityException | WalletCoreException | WalletException | InterruptedException |
                 ExecutionException e) {
            CaLog.d("requestWalletTokenData ex : " + e.getMessage());
            throw new CompletionException(e);
        }
        return completableFuture;
    }

    protected CompletableFuture<ProtocolData> requestAttestedAppInfo(ProtocolData protocolData) {

        CompletableFuture<ProtocolData> completableFuture = new CompletableFuture<>();
        try {
            JSONObject json = new JSONObject();
            json.put("appId", Preference.getCaAppId(context));
            String result = new HttpUrlConnection().send(context, Config.CAS.REQUEST_ATTESTED_APPINFO, "POST", json.toString());
            protocolData.setAttestedResult(result);
            CaLog.d("requestAttestedAppInfo success : " + result);

            completableFuture.complete(protocolData);
        } catch (JSONException e) {
            throw new CompletionException(e);
        }
        return completableFuture;
    }

    protected CompletableFuture<ProtocolData> requestCreateToken(ProtocolData protocolData) {
        CaLog.d("requestCreateToken");
        CompletableFuture<ProtocolData> completableFuture = new CompletableFuture<>();
        try {
            ServerTokenSeed serverTokenSeed = new ServerTokenSeed();
            serverTokenSeed.setPurpose(ServerTokenPurpose.SERVER_TOKEN_PURPOSE.UPDATE_DID);

            WalletApi walletApi = WalletApi.getInstance(context);
            SignedWalletInfo signedWalletInfo = walletApi.getSignedWalletInfo();
            serverTokenSeed.setWalletInfo(signedWalletInfo);
            AttestedAppInfo attestedAppInfo = MessageUtil.deserialize(protocolData.getAttestedResult(), AttestedAppInfo.class);
            serverTokenSeed.setCaAppInfo(attestedAppInfo);

            CreateTokenReqVO createTokenReqVO = new CreateTokenReqVO();
            createTokenReqVO.setId(CaUtil.createMessageId(context));
            createTokenReqVO.setTxId(protocolData.getTxId());
            createTokenReqVO.setSeed(serverTokenSeed);

            String result = new HttpUrlConnection().send(context, Config.TAS.REQUEST_CREATE_TOKEN, "POST", createTokenReqVO.toJson());
            CaLog.d("requestCreateToken success : " + result);
            if (org.omnione.did.sdk.wallet.walletservice.config.Config.isMlKemKeyAgreement()) {
                protocolData.sethServerToken(TokenUtil.createServerTokenMlKem(result,
                        protocolData.getMlKemResult(),
                        protocolData.getClientNonce(),
                        protocolData.getMlKemSharedSecret()));
            } else {
                protocolData.sethServerToken(TokenUtil.createServerToken(result,
                        protocolData.getEcdhResult(),
                        protocolData.getClientNonce(),
                        protocolData.getDhKeyPair()));
            }
            CaLog.d("requestCreateToken serverToken : " + protocolData.gethServerToken());
            completableFuture.complete(protocolData);

        } catch (WalletException | UtilityException | WalletCoreException e) {
            CaLog.d("requestCreateToken ex : " + e.getMessage());
            throw new CompletionException(e);
        }
        return completableFuture;
    }
}
