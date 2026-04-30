/*
 * Copyright 2025 OmniOne.
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

package org.omnione.did.tas.v1.agent.dto.entity;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * 성능 측정용 키교환 단독 테스트 요청 DTO.
 *
 * 기존 /request-ecdh 는 Transaction/SubTransaction 검증과 서명 검증이 강제되어
 * 단독 호출이 불가하므로, 순수한 키교환 연산 시간만 측정하기 위한 최소 필드만 받는다.
 *
 * - ECDH 모드: curve ("Secp256r1") + 클라이언트 임시 EC 공개키 (multibase 압축형)
 * - ML-KEM-768 모드: algorithm ("ML-KEM-768") + 클라이언트 임시 ML-KEM 공개키 (multibase X.509)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class TestRequestEcdhOnlyReqDto {
    @NotNull(message = "client cannot be null")
    private String client;

    @NotNull(message = "clientNonce cannot be null")
    private String clientNonce;

    private String curve;

    private String algorithm;

    @NotNull(message = "publicKey cannot be null")
    private String publicKey;
}
