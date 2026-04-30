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

package org.omnione.did.sdk.core.bioprompthelper;

import android.content.Context;

import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.concurrent.Executor;

import javax.crypto.Cipher;

public class BioPromptHelper {
    private Context context;
    private final int ERROR_FAILED = 7;
    private final int ERROR_CANCELED_KEY = 10;
    private final int ERROR_CANCELED_BUTTON = 13;
    private Cipher lastAuthenticatedCipher;
    public BioPromptHelper(){}
    public BioPromptHelper(Context context){
        this.context = context;
    }
    public void setBioPromptListener(BioPromptInterface bioPromptInterface){
        this.bioPromptInterface = bioPromptInterface;
    }
    public interface BioPromptInterface {
        void onSuccess(String result);
        void onFail(String result);
        void onError(String result);
        void onCancel(String result);
    }
    private BioPromptInterface bioPromptInterface;

    /**
     * Returns the Cipher that was authenticated by the most recent successful biometric prompt.
     * Only meaningful after a {@link #registerBioKey(Context, String, BiometricPrompt.CryptoObject)}
     * or {@link #authenticateBioKey(Context, String, BiometricPrompt.CryptoObject)} success.
     * Cleared by {@link #clearLastAuthenticatedCipher()}.
     */
    public Cipher getLastAuthenticatedCipher() {
        return lastAuthenticatedCipher;
    }

    public void clearLastAuthenticatedCipher() {
        this.lastAuthenticatedCipher = null;
    }

    /**
     * register a fingerprint for signing key
     * @param ctx
     */
    public void registerBioKey(Context ctx, String Message) {
        registerBioKey(ctx, Message, null);
    }

    /**
     * Register variant that binds the prompt to a {@link BiometricPrompt.CryptoObject}.
     * On success, the authenticated Cipher is stashed and retrievable via
     * {@link #getLastAuthenticatedCipher()}.
     */
    public void registerBioKey(Context ctx, String Message, BiometricPrompt.CryptoObject cryptoObject) {
        showPrompt(ctx, cryptoObject);
    }

    /**
     * authenticates a fingerprint for signing key
     * @param ctx
     */
    public void authenticateBioKey(Context ctx, String Message) {
        authenticateBioKey(ctx, Message, null);
    }

    /**
     * Authenticate variant that binds the prompt to a {@link BiometricPrompt.CryptoObject}.
     * On success, the authenticated Cipher is stashed and retrievable via
     * {@link #getLastAuthenticatedCipher()}.
     */
    public void authenticateBioKey(Context ctx, String Message, BiometricPrompt.CryptoObject cryptoObject) {
        showPrompt(ctx, cryptoObject);
    }

    private void showPrompt(Context ctx, BiometricPrompt.CryptoObject cryptoObject) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
            return;
        }
        lastAuthenticatedCipher = null;
        Executor executor = ContextCompat.getMainExecutor(ctx);
        BiometricPrompt biometricPrompt = new BiometricPrompt((androidx.fragment.app.FragmentActivity) ctx, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode == ERROR_FAILED)
                    bioPromptInterface.onError("onAuthenticationError");
                else if (errorCode == ERROR_CANCELED_BUTTON || errorCode == ERROR_CANCELED_KEY)
                    bioPromptInterface.onCancel("onAuthenticationCancel");
                else
                    bioPromptInterface.onError("UnknownError");
            }

            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                if (result.getCryptoObject() != null) {
                    lastAuthenticatedCipher = result.getCryptoObject().getCipher();
                }
                bioPromptInterface.onSuccess("onAuthenticationSucceeded");
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                bioPromptInterface.onFail("onAuthenticationFailed");
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Authenticate to access your private key")
                .setNegativeButtonText("Cancel")
                .build();

        ContextCompat.getMainExecutor(ctx).execute(() -> {
            if (cryptoObject != null) {
                biometricPrompt.authenticate(promptInfo, cryptoObject);
            } else {
                biometricPrompt.authenticate(promptInfo);
            }
        });
    }
}
