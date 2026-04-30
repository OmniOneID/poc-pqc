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

import org.omnione.did.base.constants.DidDocStatus;
import jakarta.persistence.*;
import lombok.*;
import org.omnione.did.data.model.enums.vc.RoleType;

import java.time.Instant;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "did")
public class Did extends BaseEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "did", nullable = false, length = 50)
    private String did;

    @Column(name = "version", nullable = false, columnDefinition = "SMALLINT")
    private Short version;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private RoleType role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private DidDocStatus status;

    @Column(name = "terminated_time")
    private Instant terminatedTime;
}
