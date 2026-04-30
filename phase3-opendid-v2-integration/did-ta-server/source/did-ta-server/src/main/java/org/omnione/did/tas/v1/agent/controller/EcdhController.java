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

package org.omnione.did.tas.v1.agent.controller;

import org.omnione.did.base.constants.UrlConstant;
import org.omnione.did.tas.v1.agent.dto.entity.RequestECDHReqDto;
import org.omnione.did.tas.v1.agent.dto.entity.RequestECDHResDto;
import org.omnione.did.tas.v1.agent.dto.entity.TestRequestEcdhOnlyReqDto;
import org.omnione.did.tas.v1.agent.dto.entity.TestRequestEcdhOnlyResDto;
import org.omnione.did.tas.v1.agent.service.EcdhService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * The EcdhController class is a controller that handles requests related to ecdh.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = UrlConstant.Tas.AGENT_V1)
public class EcdhController {
    private final EcdhService ecdhService;

    /**
     * Handles requests to initiate an ECDH operation.
     *
     * @param requestECDHReqDto The request data for the ECDH operation.
     * @return the response of the ECDH operation.
     */
    @RequestMapping(value = "/request-ecdh", method = RequestMethod.POST)
    @ResponseBody
    public RequestECDHResDto requestECDH(@Valid @RequestBody RequestECDHReqDto requestECDHReqDto) {
        return ecdhService.requestECDH(requestECDHReqDto);
    }

    /**
     * 성능 측정용 키교환 단독 엔드포인트.
     * 트랜잭션/서명 검증을 수행하지 않고 ECDH 또는 ML-KEM-768 연산만 실행한 뒤
     * 서버 처리 시간(ms)을 반환한다. PQC 성능 비교 측정 전용.
     */
    @RequestMapping(value = "/test/request-ecdh-only", method = RequestMethod.POST)
    @ResponseBody
    public TestRequestEcdhOnlyResDto requestEcdhOnly(@Valid @RequestBody TestRequestEcdhOnlyReqDto req) {
        return ecdhService.requestECDHOnly(req);
    }
}
