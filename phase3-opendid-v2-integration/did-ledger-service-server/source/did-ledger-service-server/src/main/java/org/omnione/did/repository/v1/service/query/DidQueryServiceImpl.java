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

import lombok.RequiredArgsConstructor;
import org.omnione.did.base.constants.DidDocStatus;
import org.omnione.did.base.db.domain.Did;
import org.omnione.did.base.db.domain.DidDocument;
import org.omnione.did.base.db.domain.DidDocumentRevoked;
import org.omnione.did.base.db.domain.DidDocumentStatusHistory;
import org.omnione.did.base.db.repository.DidDocumentRepository;
import org.omnione.did.base.db.repository.DidDocumentRevokedRepository;
import org.omnione.did.base.db.repository.DidDocumentStatusHistoryRepository;
import org.omnione.did.base.db.repository.DidRepository;
import org.omnione.did.base.exception.ErrorCode;
import org.omnione.did.base.exception.OpenDidException;
import org.omnione.did.data.model.enums.vc.RoleType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Description...
 *
 * @author : gwnam
 * @fileName : DidQueryServiceImpl
 * @since : 6/14/24
 */
@RequiredArgsConstructor
@Service
public class DidQueryServiceImpl implements DidQueryService {
    private final DidRepository didRepository;
    private final DidDocumentRepository didDocumentRepository;
    private final DidDocumentStatusHistoryRepository didDocumentStatusHistoryRepository;
    private final DidDocumentRevokedRepository didDocumentRevokedRepository;

    @Override
    public Did save(Did did) {
        return didRepository.save(did);
    }

    @Override
    public DidDocument save(DidDocument didDocument) {
        return didDocumentRepository.save(didDocument);
    }

    @Override
    public Optional<Did> didFindByDid(String did) {
        return didRepository.findByDid(did);
    }

    @Override
    public Optional<DidDocument> didDocFindByDidIdAndVersion(Long didId, Short version) {
        return didDocumentRepository.findByDidIdAndVersion(didId, version);
    }

    @Override
    public Optional<DidDocument> didDocRevokedFindByDidIdAndVersion(Long didId, Short version) {
        return didDocumentRevokedRepository.findByDidIdAndVersion(didId, version);
    }

    @Override
    public void updateDeactivateByDidEntity(Long didId, Boolean deactivated) {
        didDocumentRepository.updateStatusByDidEntity(didId, deactivated);
    }

    @Override
    public Optional<DidDocument> findFirstByDidIdOrderByIdDesc(Long didId) {
        return didDocumentRepository.findFirstByDidIdOrderByIdDesc(didId);
    }

    @Override
    public DidDocumentStatusHistory save(DidDocumentStatusHistory didDocumentStatusHistory) {
        return didDocumentStatusHistoryRepository.save(didDocumentStatusHistory);
    }

    @Override
    public List<DidDocument> findAllByDidId(Long didId) {
        return didDocumentRepository.findAllByDidId(didId);
    }


    @Override
    public void revokeDidDocument(Long didId) {
        List<DidDocument> didDocumentList = findAllByDidId(didId);
        List<DidDocumentRevoked> didDocumentRevokedList = didDocumentList.stream().map(didDocument -> DidDocumentRevoked.builder()
                        .version(didDocument.getVersion())
                        .document(didDocument.getDocument())
                        .controller(didDocument.getController())
                        .deactivated(Boolean.TRUE)
                        .didId(didDocument.getDidId())
                        .build())
                .collect(Collectors.toList());
        List<DidDocumentStatusHistory> revokedDidDocumentList = didDocumentList.stream().map(didDocument -> DidDocumentStatusHistory.builder()
                        .version(didDocument.getVersion())
                        .didId(didDocument.getDidId())
                        .toStatus(DidDocStatus.REVOKED)
                        .changedAt(Instant.now())
                        .reason("Revoked DID Document")
                        .didId(didDocument.getDidId())
                        .build())
                .collect(Collectors.toList());

        didDocumentRevokedRepository.saveAll(didDocumentRevokedList);
        didDocumentRepository.deleteAllByDidId(didId);
        didDocumentStatusHistoryRepository.saveAll(revokedDidDocumentList);
    }

    @Override
    public Optional<DidDocument> findDidDocFirstByDidAndVersion(Long didId, Short version) {
        return didDocumentRepository.findByDidIdAndVersion(didId, version);
    }

    @Override
    public Did findDidFirstByRole(RoleType roleType) {
        return didRepository.findByRole(roleType)
                .orElseThrow(() -> new OpenDidException(ErrorCode.ROLE_DID_NOT_FOUND));
    }

    @Override
    public boolean existDid() {
        return didRepository.count() != 0;
    }
}
