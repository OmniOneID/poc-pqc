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

package org.omnione.did.tas.v1.agent.service;

import org.omnione.did.tas.v1.agent.dto.entity.RequestECDHReqDto;
import org.omnione.did.tas.v1.agent.dto.entity.RequestECDHResDto;
import org.omnione.did.tas.v1.agent.dto.entity.TestRequestEcdhOnlyReqDto;
import org.omnione.did.tas.v1.agent.dto.entity.TestRequestEcdhOnlyResDto;

/**
 * ECDH service interface for managing ECDH.
 */
public interface EcdhService {
    RequestECDHResDto requestECDH(RequestECDHReqDto requestECDHReqDto);

    /**
     * 성능 측정용 키교환 단독 호출.
     * 트랜잭션/서명 검증 없이 ECDH 또는 ML-KEM-768 연산만 수행하고 소요 시간을 반환한다.
     */
    TestRequestEcdhOnlyResDto requestECDHOnly(TestRequestEcdhOnlyReqDto req);
}
