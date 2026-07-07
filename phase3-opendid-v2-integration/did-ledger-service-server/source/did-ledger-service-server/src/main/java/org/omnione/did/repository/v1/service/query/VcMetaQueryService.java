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

import org.omnione.did.base.db.domain.VcMetadata;
import org.omnione.did.base.db.domain.VcStatusHistory;
import org.omnione.did.base.db.repository.VcMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.omnione.did.base.db.repository.VcStatusHistoryRepository;
import org.springframework.stereotype.Service;

/**
 * Description...
 *
 * @author : gwnam
 * @fileName : VcMetaQueryService
 * @since : 6/12/24
 */
@RequiredArgsConstructor
@Service
public class VcMetaQueryService {
    private final VcMetadataRepository vcMetadataRepository;
    private final VcStatusHistoryRepository vcStatusHistoryRepository;

    public VcMetadata save(VcMetadata vcMetadata) {
        return vcMetadataRepository.save(vcMetadata);
    }

    public VcMetadata findByVcId(String vcId) {
        return vcMetadataRepository.findByVcId(vcId)
                .orElseThrow(IllegalArgumentException::new);
    }

    public VcStatusHistory save(VcStatusHistory vcStatusHistory) {
        return vcStatusHistoryRepository.save(vcStatusHistory);
    }
}
