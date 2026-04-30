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
package org.omnione.did.repository.v1.controller;

import org.omnione.did.base.constants.UrlConstant;
import org.omnione.did.repository.v1.dto.common.EmptyResDto;
import org.omnione.did.repository.v1.dto.did.InputDidDocReqDto;
import org.omnione.did.repository.v1.dto.did.UpdateDidDocReqDto;
import org.omnione.did.repository.v1.service.DidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = UrlConstant.LSS + UrlConstant.Did.V1)
public class DidController {

    private final DidService didService;

    @PostMapping
    public void generateDid(@RequestBody InputDidDocReqDto request) {
        didService.generateDid(request);
    }

    @GetMapping
    public String getDid(@RequestParam(value = "did", defaultValue = "did:omn:tas") String didDoc) {
        return didService.getDid(didDoc);
    }


    // TODO 기능 개발
    @PatchMapping
    public void updateDidStatus(@RequestBody UpdateDidDocReqDto request) {
        didService.updateStatus(request);
    }
}
