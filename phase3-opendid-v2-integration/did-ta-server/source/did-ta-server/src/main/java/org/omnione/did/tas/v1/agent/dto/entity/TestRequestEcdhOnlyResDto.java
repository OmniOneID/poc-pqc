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

import lombok.*;

/**
 * 성능 측정용 키교환 단독 테스트 응답 DTO.
 *
 * serverProcessingMs: 서버 측 키교환 + 서명 소요 시간(ms)
 * ciphertext:         ML-KEM 경로에서 클라이언트 decapsulation에 필요한 암호문 (ECDH 경로에서는 null)
 * serverPublicKey:    ECDH 경로에서 클라이언트 shared secret 계산에 필요한 서버 공개키 (ML-KEM 경로에서는 null)
 * proofValue:         TAS가 AccEcdh 해시에 대해 생성한 ECDSA 서명 (compact, multibase 인코딩)
 * tasKeyAgreePublicKey: TAS keyagree 압축 공개키 (multibase 인코딩) — 클라이언트 서명 검증에 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class TestRequestEcdhOnlyResDto {
    private double serverProcessingMs;
    private String result;
    private String algorithm;
    /** ML-KEM: 클라이언트 decapsulation용 암호문. ECDH 경로에서는 null. */
    private String ciphertext;
    /** ECDH: 클라이언트 shared secret 계산용 서버 압축 공개키. ML-KEM 경로에서는 null. */
    private String serverPublicKey;
    /** TAS가 dummyHash(32-byte zeros)에 서명한 compact ECDSA 서명값 (multibase). */
    private String proofValue;
    /** 클라이언트가 proofValue를 검증할 때 사용하는 TAS keyagree 압축 공개키 (multibase). */
    private String tasKeyAgreePublicKey;
}
