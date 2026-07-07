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

package org.omnione.did.base.datamodel.data;

import org.omnione.did.base.datamodel.enums.SymmetricCipherType;
import org.omnione.did.base.validation.NonEmptyList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the Candidate structure.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Candidate {
    @NonEmptyList(message = "candidate.ciphers must not be empty")
    private ArrayList<SymmetricCipherType> ciphers;

    // PQC: 클라이언트가 지원하는 키 교환 알고리즘 목록 (e.g. ["ML-KEM-768", "Secp256r1"])
    // null 또는 생략 시 algorithm/curve 필드로 단일 알고리즘 지정한 것으로 간주
    private List<String> keyAgreements;
}
