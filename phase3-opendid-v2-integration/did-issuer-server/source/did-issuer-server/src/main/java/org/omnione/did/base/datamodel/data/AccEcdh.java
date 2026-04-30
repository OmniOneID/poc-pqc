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

package org.omnione.did.base.datamodel.data;

import lombok.*;
import org.omnione.did.base.datamodel.enums.SymmetricCipherType;
import org.omnione.did.base.datamodel.enums.SymmetricPaddingType;
import org.omnione.did.data.model.did.Proof;

/**
 * Represents the Accept E2E (End-to-End) data structure.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class AccEcdh {
    private String server;
    private String serverNonce;
    private String publicKey;          // ECDH 전용: 서버 임시 EC 공개키
    // PQC: ML-KEM encapsulate 결과. ML-KEM 응답 시 publicKey 대신 사용
    private String ciphertext;
    // PQC: 서버가 선택한 키 교환 알고리즘 (e.g. "ML-KEM-768"). ML-KEM 응답 시에만 설정
    private String selectedAlgorithm;
    private SymmetricCipherType cipher;
    private SymmetricPaddingType padding;
    private Proof proof;

}
