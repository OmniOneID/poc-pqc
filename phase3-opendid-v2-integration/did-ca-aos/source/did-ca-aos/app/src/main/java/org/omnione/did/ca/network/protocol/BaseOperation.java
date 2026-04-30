package org.omnione.did.ca.network.protocol;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.omnione.did.ca.config.Config;
import org.omnione.did.ca.config.Constants;
import org.omnione.did.ca.config.Preference;
import org.omnione.did.ca.logger.CaLog;
import org.omnione.did.ca.network.HttpUrlConnection;
import org.omnione.did.ca.network.protocol.token.CreateTokenReqVO;
import org.omnione.did.ca.util.CaUtil;
import org.omnione.did.ca.util.TokenUtil;
import org.omnione.did.sdk.communication.exception.CommunicationException;
import org.omnione.did.sdk.core.api.WalletApi;
import org.omnione.did.sdk.core.exception.WalletCoreException;
import org.omnione.did.sdk.datamodel.common.enums.ServerTokenPurpose;
import org.omnione.did.sdk.datamodel.common.enums.SymmetricCipherType;
import org.omnione.did.sdk.datamodel.common.enums.WalletTokenPurpose;
import org.omnione.did.sdk.datamodel.protocol.P132RequestVo;
import org.omnione.did.sdk.datamodel.security.ReqEcdh;
import org.omnione.did.sdk.datamodel.token.AttestedAppInfo;
import org.omnione.did.sdk.datamodel.token.ServerTokenSeed;
import org.omnione.did.sdk.datamodel.token.SignedWalletInfo;
import org.omnione.did.sdk.datamodel.token.WalletTokenSeed;
import org.omnione.did.sdk.datamodel.util.MessageUtil;
import org.omnione.did.sdk.datamodel.common.enums.EllipticCurveType;
import org.omnione.did.sdk.utility.CryptoUtils;
import org.omnione.did.sdk.utility.DataModels.EcKeyPair;
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

    protected String sendTimed(ProtocolData protocolData, String url, String method, String request) {
        long startNs = System.nanoTime();
        String result = new HttpUrlConnection().send(context, url, method, request);
        if (protocolData != null) {
            protocolData.addServerRoundTripNs(System.nanoTime() - startNs);
        }
        return result;
    }

    protected CompletableFuture<ProtocolData> requestECDH(ProtocolData protocolData) {
        CaLog.d("requestECDH");
        CompletableFuture<ProtocolData> completableFuture = new CompletableFuture<>();
        try {
            P132RequestVo requestVo = new P132RequestVo(CaUtil.createMessageId(context), protocolData.getTxId());
            ReqEcdh reqEcdh = new ReqEcdh();
            WalletApi walletApi = WalletApi.getInstance(context);
            // 사용자 등록(P132) 전에는 holder DID가 없으므로 device DID 사용
            // 이후 모든 프로토콜(VC발급, VP제출 등)은 holder DID 사용 (TAS가 holder DID로 사용자 조회)
            int didType = (operationType == OperationType.USER_REGISTRATION)
                    ? Constants.DID_TYPE_DEVICE
                    : Constants.DID_TYPE_HOLDER;
            String did = walletApi.getDIDDocument(didType).getId();
            reqEcdh.setClient(did);
            protocolData.setClientNonce(CryptoUtils.generateNonce(16));

            String keyAlgorithm = org.omnione.did.sdk.wallet.walletservice.config.Config.KEY_ALGORITHM;
            reqEcdh.setClientNonce(MultibaseUtils.encode(MultibaseType.MULTIBASE_TYPE.BASE_58_BTC, protocolData.getClientNonce()));

            if ("MlDsa44".equals(keyAlgorithm)) {
                // PQC: ECDH 임시 키페어 대신 ML-KEM-768 임시 키페어 생성
                CaLog.d("use ml-kem-768 ephemeral keypair");
                CryptoUtils.MlKemKeyPair kemKeyPair = CryptoUtils.generateMLKEMKeyPair();
                protocolData.setKemPrivateKey(kemKeyPair.getPrivateKey());
                // PQC: curve 대신 algorithm 필드로 ML-KEM-768 명시
                reqEcdh.setAlgorithm("ML-KEM-768");
                // PQC: ML-KEM 공개키는 X.509 인코딩 후 multibase 인코딩
                reqEcdh.setPublicKey(MultibaseUtils.encode(MultibaseType.MULTIBASE_TYPE.BASE_58_BTC, kemKeyPair.getPublicKey()));
                // PQC: keyAgreements 협상 목록 포함
                reqEcdh.setCandidate(new ReqEcdh.Ciphers(
                        List.of(SymmetricCipherType.SYMMETRIC_CIPHER_TYPE.AES256CBC),
                        List.of("ML-KEM-768", "Secp256r1")
                ));
            } else {
                // Classic: Secp256r1 ECDH 임시 키페어 생성
                CaLog.d("use secp256r1 ecdh ephemeral keypair");
                EcKeyPair dhKeyPair = CryptoUtils.generateECKeyPair(EcType.EC_TYPE.SECP256_R1);
                protocolData.setDhKeyPair(dhKeyPair);
                protocolData.setKemPrivateKey(null);
                reqEcdh.setCurve(EllipticCurveType.ELLIPTIC_CURVE_TYPE.SECP256R1);
                reqEcdh.setPublicKey(dhKeyPair.getPublicKey());
            }
            reqEcdh = (ReqEcdh) walletApi.addProofsToDocument(reqEcdh, List.of("keyagree"), did, didType, null, false);
            requestVo.setReqEcdh(reqEcdh);

            String result = sendTimed(protocolData, Config.TAS.REQUEST_ECDH, "POST", requestVo.toJson());
            protocolData.setEcdhResult(result);
            CaLog.d("ECDH success : " + protocolData.getEcdhResult());

            completableFuture.complete(protocolData);

        } catch (WalletException | UtilityException | WalletCoreException e) {
            CaLog.d("ECDH ex : " + e.getMessage());
            throw new CompletionException(e);
        }

        return completableFuture;
    }

    private WalletTokenPurpose.WALLET_TOKEN_PURPOSE resolveWalletTokenPurpose() {
        switch (operationType) {
            case USER_REGISTRATION: return WalletTokenPurpose.WALLET_TOKEN_PURPOSE.CREATE_DID;
            case USER_UPDATE:       return WalletTokenPurpose.WALLET_TOKEN_PURPOSE.UPDATE_DID;
            case VC_ISSUERANCE:     return WalletTokenPurpose.WALLET_TOKEN_PURPOSE.ISSUE_VC;
            case VP_SUBMISSION:     return WalletTokenPurpose.WALLET_TOKEN_PURPOSE.LIST_VC_AND_PRESENT_VP;
            default:                return WalletTokenPurpose.WALLET_TOKEN_PURPOSE.CREATE_DID;
        }
    }

    private ServerTokenPurpose.SERVER_TOKEN_PURPOSE resolveServerTokenPurpose() {
        switch (operationType) {
            case USER_REGISTRATION: return ServerTokenPurpose.SERVER_TOKEN_PURPOSE.CREATE_DID;
            case USER_UPDATE:       return ServerTokenPurpose.SERVER_TOKEN_PURPOSE.UPDATE_DID;
            case VC_ISSUERANCE:     return ServerTokenPurpose.SERVER_TOKEN_PURPOSE.ISSUE_VC;
            case VP_SUBMISSION:     return ServerTokenPurpose.SERVER_TOKEN_PURPOSE.PRESENT_VP;
            default:                return ServerTokenPurpose.SERVER_TOKEN_PURPOSE.CREATE_DID;
        }
    }

    protected CompletableFuture<ProtocolData> requestWalletTokenData(ProtocolData protocolData) {
        CaLog.d("requestWalletTokenData");
        CompletableFuture<ProtocolData> completableFuture = new CompletableFuture<>();
        try {
            WalletApi walletApi = WalletApi.getInstance(context);
            WalletTokenSeed walletTokenSeed = walletApi.createWalletTokenSeed(resolveWalletTokenPurpose(),
                    CaUtil.getPackageName(context),
                    Preference.getUserIdForDemo(context));
            String result = sendTimed(protocolData, Config.CAS.REQUEST_WALLET_TOKENDATA, "POST", walletTokenSeed.toJson());
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
            String result = sendTimed(protocolData, Config.CAS.REQUEST_ATTESTED_APPINFO, "POST", json.toString());
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
            serverTokenSeed.setPurpose(resolveServerTokenPurpose());

            WalletApi walletApi = WalletApi.getInstance(context);
            SignedWalletInfo signedWalletInfo = walletApi.getSignedWalletInfo();
            serverTokenSeed.setWalletInfo(signedWalletInfo);
            AttestedAppInfo attestedAppInfo = MessageUtil.deserialize(protocolData.getAttestedResult(), AttestedAppInfo.class);
            serverTokenSeed.setCaAppInfo(attestedAppInfo);

            CreateTokenReqVO createTokenReqVO = new CreateTokenReqVO();
            createTokenReqVO.setId(CaUtil.createMessageId(context));
            createTokenReqVO.setTxId(protocolData.getTxId());
            createTokenReqVO.setSeed(serverTokenSeed);

            String result = sendTimed(protocolData, Config.TAS.REQUEST_CREATE_TOKEN, "POST", createTokenReqVO.toJson());
            CaLog.d("requestCreateToken success : " + result);
            // PQC: ML-KEM이면 kemPrivateKey 사용, ECDH이면 dhKeyPair 사용
            if (protocolData.getKemPrivateKey() != null) {
                protocolData.sethServerToken(TokenUtil.createServerToken(result,
                        protocolData.getEcdhResult(),
                        protocolData.getClientNonce(),
                        protocolData.getKemPrivateKey()));
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
