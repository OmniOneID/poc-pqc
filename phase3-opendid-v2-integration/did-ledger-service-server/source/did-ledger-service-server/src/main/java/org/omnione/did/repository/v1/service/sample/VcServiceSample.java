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
package org.omnione.did.repository.v1.service.sample;

import org.omnione.did.crypto.enums.MultiBaseType;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.data.model.vc.VcMeta;
import org.omnione.did.repository.v1.dto.vc.TssGetVcMetaResDto;
import org.omnione.did.repository.v1.dto.vc.UpdateVcStatusReqDto;
import org.omnione.did.repository.v1.service.VcService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Profile("sample")
@Service
public class VcServiceSample implements VcService {
    @Override
    public void inputVcMeta(VcMeta request) {

    }


    @Override
    public String getVcMetaByVcId(String vcId) {
        String vcMeta = """
                                    {
                  "id": "99999999-9999-9999-9999-999999999999",
                  "issuer": {
                    "did": "did:raon:issuer",
                    "name": "issuer"
                  },
                  "subject": "Subject did",
                  "credentialSchema": {
                    "id": "http://192.168.3.130:8090/tas/api/v1/download/schema?name=mdl",
                    "type": "OsdSchemaCredential"
                  },
                  "status": "VC 상태",
                  "issuanceDate": "2024-01-01T00:00:00Z",
                  "validFrom": "2024-01-01T00:00:00Z",
                  "validUntil": "2099-01-01T00:00:00Z",
                  "formatVersion": "1.0",
                  "language": "ko"
                }
                                    """;

        return vcMeta;
    }

    @Override
    public void updateVc(UpdateVcStatusReqDto request) {

    }
}
