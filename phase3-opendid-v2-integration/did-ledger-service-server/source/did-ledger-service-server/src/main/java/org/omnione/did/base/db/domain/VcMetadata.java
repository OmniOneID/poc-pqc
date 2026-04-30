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
package org.omnione.did.base.db.domain;

import jakarta.persistence.*;
import lombok.*;
import org.omnione.did.data.model.enums.vc.VcStatus;

import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "vc_metadata")
public class VcMetadata extends BaseEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vc_id", nullable = false, length = 100)
    private String vcId;

    @Column(name = "issuer_did", nullable = false, length = 100)
    private String issuerDid;

    @Column(name = "subject_did", nullable = false, length = 100)
    private String subjectDid;

    @Column(name = "vc_schema", nullable = false, length = 200)
    private String vcSchema;

    @Column(name = "issuance_date", nullable = false)
    private String issuanceDate;

    @Column(name = "valid_from", nullable = false)
    private String validFrom;

    @Column(name = "valid_until", nullable = false)
    private String validUntil;

    @Column(name = "format_version")
    private String formatVersion;

    @Column(name = "language")
    private String language;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "metadata", nullable = false)
    private String metadata;
}
