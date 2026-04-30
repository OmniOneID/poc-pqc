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
import org.omnione.did.repository.v1.dto.did.InputDidDocReqDto;
import org.omnione.did.repository.v1.dto.did.TssGetDidDocResDto;
import org.omnione.did.repository.v1.dto.did.UpdateDidDocReqDto;
import org.omnione.did.repository.v1.service.DidService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Profile("sample")
@Service
public class DidServiceSample implements DidService {
    @Override
    public void generateDid(InputDidDocReqDto request) {

    }

    @Override
    public String getDid(String did) {

        String didDoc = """
                                    {
                      "@context": ["https://www.w3.org/ns/did/v1"],
                      "id": "did:raon:user1",
                      "controller": "did:raon:user1",
                      "created": "2024-01-01T09:00:00Z",
                      "updated": "2024-01-01T09:00:00Z",
                      "versionId": "1",
                      "deactivated": false,
                      "verificationMethod": [
                        {
                          "id": "pin",
                          "type": "Secp256r1VerificationKey2018",
                          "controller": "did:raon:user1",
                          "publicKeyMultibase": "z3EFKFf4xRAjfryP52YK71HN8o5VhbJKuvG5qNyuwFo3XT68fw5jrADosNf52pGs36RpWjpfEgLSTjsRjtpzVjW3j21yNE83ZND3A5TERCtyhC6iqfPPaTdijZ9giFnZ7SioGP8YixnhBXxMgC2GFsrNvt2afWqnsYuyURWbsBShAzRY4eHpU9kkhX5gKFxNSEyJAjPmuyc4TXCXd6fRtrUeC55PiEjFRqRRSGHxes1XvcU",
                          "authType": 1
                        },
                        {
                          "id": "bio",
                          "type": "Secp256r1VerificationKey2018",
                          "controller": "did:raon:user1",
                          "publicKeyMultibase": "z3EFKFf4xRAjfryP52YK71HN8o5VhbJKuvG5qNyuwFo3XT68fw5jrADosNf52pGs36RpWjpfEgLSTjsRjtpzVjescnnZPfyGxsuVTJUh5RoJw4ofFYWciKfnkWxUqjridYxgBWnCWrL6spyBeTswbnSXwFWy5owvvy9R4rNHb5g2nrfrRz6Qh1ezLYygGU9LRXiJ31YU5XDsgBAxKHg26MBV9L4uVAsF9mwiFTqwp2R3fRv",
                          "authType": 1
                        }
                      ],
                      "assertionMethod": ["pin", "bio"],
                      "authentication": ["pin", "bio"]
                    }
                """;

        return didDoc;
    }

    @Override
    public void updateStatus(UpdateDidDocReqDto request) {

    }
}
