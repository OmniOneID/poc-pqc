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
package org.omnione.did.repository.v1.service.query;

import org.omnione.did.base.db.domain.Did;
import org.omnione.did.base.db.domain.DidDocument;
import org.omnione.did.base.db.domain.DidDocumentStatusHistory;
import org.omnione.did.data.model.enums.vc.RoleType;

import java.util.List;
import java.util.Optional;

/**
 * Description...
 *
 * @author : gwnam
 * @fileName : DidQueryService
 * @since : 6/14/24
 */
public interface DidQueryService {
    Did save(Did did);
    DidDocument save(DidDocument didDocument);
    Optional<Did> didFindByDid(String did);
    Optional<DidDocument> didDocFindByDidIdAndVersion(Long did, Short version);
    Optional<DidDocument> didDocRevokedFindByDidIdAndVersion(Long did, Short version);
    void updateDeactivateByDidEntity(Long didId, Boolean deactivated);
    Optional<DidDocument> findFirstByDidIdOrderByIdDesc(Long didId);

    DidDocumentStatusHistory save(DidDocumentStatusHistory didDocumentStatusHistory);

    List<DidDocument> findAllByDidId(Long didId);

    void revokeDidDocument(Long didId);

    Optional<DidDocument> findDidDocFirstByDidAndVersion(Long id, Short version);

    Did findDidFirstByRole(RoleType roleType);

    boolean existDid();

}
