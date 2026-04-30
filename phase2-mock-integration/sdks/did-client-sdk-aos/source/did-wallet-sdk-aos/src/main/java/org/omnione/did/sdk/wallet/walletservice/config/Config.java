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

package org.omnione.did.sdk.wallet.walletservice.config;

import org.omnione.did.sdk.datamodel.common.enums.AlgorithmType;

public class Config {
    public final static String API_GATEWAY_GET_DID_DOC = "/api-gateway/api/v1/did-doc?did=";
    public final static String TAS_GET_ALLOWED_CA = "/list/api/v1/allowed-ca/list?wallet=";
    public final static String WALLET_PKG_NAME = "org.omnione.did.sdk.wallet";
    public final static String DID_METHOD = "omn";
    public final static String DID_CONTROLLER = "did:omn:tas";

    /**
     * 서명 알고리즘 설정.
     * - SECP256R1: 기존 ECC 기반 (기본값)
     * - ML_DSA_44: PQC ML-DSA-44 (FIPS 204)
     *
     * 이 값을 변경하면 키 생성, 서명, proof type, DID document key type이 모두 전환됩니다.
     * 주의: 알고리즘 변경 후에는 기존 키/DID를 초기화(재등록)해야 합니다.
     */
    private static AlgorithmType.ALGORITHM_TYPE SIGNATURE_ALGORITHM = AlgorithmType.ALGORITHM_TYPE.ML_DSA_44;

    /**
     * Key Agreement 알고리즘 설정.
     * - SECP256R1: 기존 ECDH 기반 (기본값)
     * - ML_KEM_768: PQC ML-KEM-768 (FIPS 203)
     *
     * 이 값을 변경하면 keyagree 키 생성, E2E 채널 수립 방식이 전환됩니다.
     * 주의: 알고리즘 변경 후에는 기존 키/DID를 초기화(재등록)해야 합니다.
     */
    private static AlgorithmType.ALGORITHM_TYPE KEY_AGREEMENT_ALGORITHM = AlgorithmType.ALGORITHM_TYPE.ML_KEM_768;

    public static AlgorithmType.ALGORITHM_TYPE getSignatureAlgorithm() {
        return SIGNATURE_ALGORITHM;
    }

    public static void setSignatureAlgorithm(AlgorithmType.ALGORITHM_TYPE algorithm) {
        SIGNATURE_ALGORITHM = algorithm;
    }

    public static AlgorithmType.ALGORITHM_TYPE getKeyAgreementAlgorithm() {
        return KEY_AGREEMENT_ALGORITHM;
    }

    public static void setKeyAgreementAlgorithm(AlgorithmType.ALGORITHM_TYPE algorithm) {
        KEY_AGREEMENT_ALGORITHM = algorithm;
    }

    public static boolean isMlKemKeyAgreement() {
        return KEY_AGREEMENT_ALGORITHM == AlgorithmType.ALGORITHM_TYPE.ML_KEM_768;
    }
}
