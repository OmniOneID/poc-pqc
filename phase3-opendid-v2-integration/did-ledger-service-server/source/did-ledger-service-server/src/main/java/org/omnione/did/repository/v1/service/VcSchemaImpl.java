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
package org.omnione.did.repository.v1.service;

import com.google.gson.JsonSyntaxException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnione.did.base.constants.DidDocStatus;
import org.omnione.did.base.db.domain.Did;
import org.omnione.did.base.db.domain.VcSchemaInfo;
import org.omnione.did.base.exception.ErrorCode;
import org.omnione.did.base.exception.OpenDidException;
import org.omnione.did.base.util.BaseMultibaseUtil;
import org.omnione.did.data.model.enums.vc.RoleType;
import org.omnione.did.data.model.schema.VcSchema;
import org.omnione.did.repository.v1.dto.vc.InputVcSchemaReqDto;
import org.omnione.did.repository.v1.service.query.DidQueryServiceImpl;
import org.omnione.did.repository.v1.service.query.VcSchemaQueryService;
import org.omnione.did.zkp.datamodel.util.GsonWrapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
@Profile("!sample")
public class VcSchemaImpl implements VcSchemaService {
    private final DidQueryServiceImpl didQueryService;
    private final VcSchemaQueryService vcSchemaQueryService;
    private final GsonWrapper gsonWrapper;

    @Override
    public void generateVcSchema(InputVcSchemaReqDto request) {
        log.debug("=== Starting generateVcSchema ===");

        // 1. decode and parse the vc schema
        VcSchema vcSchema = decodeAndParseVcSchema(request.getVcSchema());

        // 2. validate the vc schema
        validateVcSchema(vcSchema);

        // 3. check if the vc schema already exists
        validateSchemaNotExists(vcSchema.getId());

        // 4. validate the issuer DID
        Did issuerInfo = validateIssuer(request.getDid());

        // 5. save the VC Schema
        saveVcSchema(vcSchema, issuerInfo);

        log.debug("*** Finished generateVcSchema ***");
    }

    @Override
    public String getVcSchema(String schemaId) {
        log.debug("=== Starting getVcSchema ===");

        // 1. find the vc schema by schema ID
        VcSchemaInfo foundVcSchemaInfo = vcSchemaQueryService.findBySchemaId(schemaId)
                .orElseThrow(() -> new OpenDidException(ErrorCode.VC_SCHEMA_NOT_FOUND));

        String schemaJson = gsonWrapper.toJson(foundVcSchemaInfo.getSchema());

        log.debug("*** Finished getVcSchema ***");
        return schemaJson;
    }

    private VcSchema decodeAndParseVcSchema(String encodedVcSchema) {
        try {
            log.debug("\t--> Decoding Vc Schema");
            byte[] decodedData = BaseMultibaseUtil.decode(encodedVcSchema);

            return GsonWrapper.getGson().fromJson(new String(decodedData), VcSchema.class);
        } catch (JsonSyntaxException e) {
            log.error("\t--> Failed to decode or parse Credential Schema: {}", e.getMessage());
            throw new OpenDidException(ErrorCode.INVALID_VC_SCHEMA);
        } catch (Exception e) {
            log.error("\t--> Unexpected error while decoding Credential Schema: {}", e.getMessage());
            throw new OpenDidException(ErrorCode.DECODING_FAILED);
        }
    }

    private void validateSchemaNotExists(String schemaId) {
        log.debug("\t--> Checking if the VC Schema is already registered ***");
        if (vcSchemaQueryService.findBySchemaId(schemaId).isPresent()) {
            log.error("\t--> VC Schema with ID {} already exists", schemaId);
            throw new OpenDidException(ErrorCode.VC_SCHEMA_ALREADY_REGISTERED);
        }
    }

    /**
     * Validates the issuer DID and returns the issuer information.
     *
     * @param issuerDid the issuer DID to validate
     * @return the validated issuer information
     * @throws OpenDidException if the issuer is invalid or not activated
     */
    private Did validateIssuer(String issuerDid) {
        log.debug("\t--> Querying issuer information by DID ***");
        Did foundIssuerInfo = didQueryService.didFindByDid(issuerDid)
                .orElseThrow(() -> new OpenDidException(ErrorCode.DID_NOT_FOUND));

        validateIssuerRole(foundIssuerInfo, issuerDid);
        validateIssuerStatus(foundIssuerInfo, issuerDid);

        return foundIssuerInfo;
    }

    /**
     * Validates that the entity has the role of Issuer, TA.
     *
     * @param issuerInfo the issuer information
     * @param issuerDid the issuer DID
     * @throws OpenDidException if the role is not Issuer
     */
    private void validateIssuerRole(Did issuerInfo, String issuerDid) {
        log.debug("\t--> Checking if the issuer has the role of Issuer ***");
        if (issuerInfo.getRole() != RoleType.ISSUER
                && issuerInfo.getRole() != RoleType.TAS) {
            log.error("\t--> The DID {} does not have the role of Issuer, TA", issuerDid);
            throw new OpenDidException(ErrorCode.ROLE_TYPE_MISMATCH);
        }
    }

    /**
     * Validates that the issuer DID is activated.
     *
     * @param issuerInfo the issuer information
     * @param issuerDid the issuer DID for logging
     * @throws OpenDidException if the DID is not activated
     */
    private void validateIssuerStatus(Did issuerInfo, String issuerDid) {
        if (issuerInfo.getStatus() != DidDocStatus.ACTIVATE) {
            log.error("\t--> The issuer DID {} is not activated", issuerDid);
            throw new OpenDidException(ErrorCode.DID_NOT_ACTIVATED);
        }
    }

    private void saveVcSchema(VcSchema vcSchema, Did issuerInfo) {
        log.debug("\t--> Saving VC Schema in the database ***");

        try {
            String schemaJson = gsonWrapper.toJson(vcSchema);

            vcSchemaQueryService.save(VcSchemaInfo.builder()
                    .schemaId(vcSchema.getId())
                    .title(vcSchema.getTitle())
                    .version(vcSchema.getMetadata().getFormatVersion())
                    .description(vcSchema.getDescription())
                    .schema(schemaJson)
                    .did(issuerInfo.getDid())
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("\t--> Failed to save VC Schema: {}", e.getMessage());
            throw new OpenDidException(ErrorCode.DB_INSERT_ERROR);
        } catch (Exception e) {
            log.error("\t--> Unexpected error while saving VC Schema: {}", e.getMessage());
            throw new OpenDidException(ErrorCode.DB_INSERT_ERROR);
        }

        log.debug("\t--> VC Schema saved successfully");
    }

    private void validateVcSchema(VcSchema vcSchema) {
        Map<String, Object> fieldsToValidate = Map.of(
                "VC Schema ID", vcSchema.getId(),
                "VC Schema content", vcSchema.getSchema(),
                "VC Schema title", vcSchema.getTitle(),
                "VC Schema description", vcSchema.getDescription(),
                "VC Schema metadata", vcSchema.getMetadata(),
                "VC Schema language", vcSchema.getMetadata() != null ? vcSchema.getMetadata().getLanguage() : null,
                "VC Schema format version", vcSchema.getMetadata() != null ? vcSchema.getMetadata().getFormatVersion() : null,
                "VC Schema credential subject", vcSchema.getCredentialSubject()
        );

        for (Map.Entry<String, Object> entry : fieldsToValidate.entrySet()) {
            if (entry.getValue() == null) {
                log.error("\t--> {} is missing", entry.getKey());
                throw new OpenDidException(ErrorCode.INVALID_VC_SCHEMA);
            }
        }
    }
}
