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
import org.omnione.did.base.db.domain.ZkpCredentialSchema;
import org.omnione.did.base.exception.ErrorCode;
import org.omnione.did.base.exception.OpenDidException;
import org.omnione.did.base.util.BaseMultibaseUtil;
import org.omnione.did.repository.v1.dto.zkp.InputZkpCredentialSchemaReqDto;
import org.omnione.did.repository.v1.service.query.DidQueryServiceImpl;
import org.omnione.did.repository.v1.service.query.ZkpCredentialSchemaQueryService;
import org.omnione.did.zkp.datamodel.schema.AttributeType;
import org.omnione.did.zkp.datamodel.schema.CredentialSchema;
import org.omnione.did.zkp.datamodel.util.GsonWrapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.omnione.did.data.model.enums.vc.RoleType;


import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
@Profile("!sample")
public class ZkpCredentialSchemaServiceImpl implements ZkpCredentialSchemaService {
    private final DidQueryServiceImpl didQueryService;
    private final ZkpCredentialSchemaQueryService zkpCredentialSchemaQueryService;
    private final GsonWrapper gsonWrapper;

    @Override
    public void generateZkpCredentialSchema(InputZkpCredentialSchemaReqDto request) {
        log.debug("=== Starting generateZkpCredentialSchema ===");

        // 1. decode and parse the credential schema
        CredentialSchema credentialSchema = decodeAndParseCredentialSchema(request.getCredentialSchema());

        // 2. validate the credential schema
        validateCredentialSchema(credentialSchema);

        // 3. extract issuer DID from the credential schema ID
        String issuerDid = extractIssuerDidFromCredentialSchemaId(credentialSchema.getId());

        // 4. check if the credential schema already exists
        validateSchemaNotExists(credentialSchema.getId());

        // 5. validate the issuer DID
        Did issuerInfo = validateIssuer(issuerDid);

        // 6. save the credential schema
        saveCredentialSchema(credentialSchema);

        log.debug("*** Finished generateZkpCredentialSchema ***");
    }

    @Override
    public String getZkpCredentialSchema(String schemaId) {
        log.debug("=== Starting getZkpCredentialSchema ===");

        // 1. find the credential schema by schema ID
        log.debug("\t--> Finding Credential Schema by schemaId: {}", schemaId);
        ZkpCredentialSchema foundZkpCredentialSchema = zkpCredentialSchemaQueryService.findBySchemaId(schemaId)
                .orElseThrow(() -> new OpenDidException(ErrorCode.CREDENTIAL_SCHEMA_NOT_FOUND));


        CredentialSchema credentialSchema = gsonWrapper.fromJson(foundZkpCredentialSchema.getSchema(), CredentialSchema.class);
        String schemaJson = credentialSchema.toJson();
        log.debug("\t--> Found Credential Schema: {}", schemaJson);

        log.debug("*** Finished getZkpCredentialSchema ***");
        return schemaJson;
    }

    /**
     * Validates that a credential schema with the given ID does not already exist.
     *
     * @param schemaId the credential schema ID to check
     * @throws OpenDidException if the schema already exists
     */
    private void validateSchemaNotExists(String schemaId) {
        log.debug("\t--> Checking if the Credential Schema is already registered ***");
        if (zkpCredentialSchemaQueryService.findBySchemaId(schemaId).isPresent()) {
            log.error("\t--> The Credential Schema with ID {} is already registered", schemaId);
            throw new OpenDidException(ErrorCode.CREDENTIAL_SCHEMA_ALREADY_REGISTERED);
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
     * Validates that the entity has the role of Issuer.
     *
     * @param issuerInfo the issuer information
     * @param issuerDid the issuer DID
     * @throws OpenDidException if the role is not Issuer
     */
    private void validateIssuerRole(Did issuerInfo, String issuerDid) {
        log.debug("\t--> Checking if the issuer has the role of Issuer ***");
        if (issuerInfo.getRole() != RoleType.ISSUER) {
            log.error("\t--> The DID {} does not have the role of Issuer", issuerDid);
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

    /**
     * Saves the credential schema to the database.
     *
     * @param credentialSchema the credential schema to save
     */
    private void saveCredentialSchema(CredentialSchema credentialSchema) {
        log.debug("\t--> Saving Credential Schema in the database ***");

        try {
            List<String> attrNames = credentialSchema.getAttrNames();
            List<AttributeType> attrTypes = credentialSchema.getAttrTypes();

            String credentialSchemaJson = gsonWrapper.toJson(credentialSchema);
            String attrNamesJson = gsonWrapper.toJson(attrNames);
            String attrTypesJson = gsonWrapper.toJson(attrTypes);

            zkpCredentialSchemaQueryService.save(ZkpCredentialSchema.builder()
                    .schemaId(credentialSchema.getId())
                    .name(credentialSchema.getName())
                    .version(credentialSchema.getVersion())
                    .tag(credentialSchema.getTag())
                    .schema(credentialSchemaJson)
                    .attrNames(attrNamesJson)
                    .attrTypes(attrTypesJson)
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("\t--> Failed to save Credential Schema: {}", e.getMessage());
            throw new OpenDidException(ErrorCode.DB_INSERT_ERROR);
        } catch (Exception e) {
            log.error("\t--> Unexpected error while saving Credential Schema: {}", e.getMessage());
            throw new OpenDidException(ErrorCode.DB_INSERT_ERROR);
        }

        log.debug("\t--> Credential Schema saved successfully");
    }

    private CredentialSchema decodeAndParseCredentialSchema(String encodedCredentialSchema) {
        try {
            log.debug("\t--> Decoding Credential Schema");
            byte[] decodedData = BaseMultibaseUtil.decode(encodedCredentialSchema);

            return GsonWrapper.getGson().fromJson(new String(decodedData), CredentialSchema.class);
        } catch (JsonSyntaxException e) {
            log.error("\t--> Failed to decode or parse Credential Schema: {}", e.getMessage());
            throw new OpenDidException(ErrorCode.INVALID_CREDENTIAL_SCHEMA);
        } catch (Exception e) {
            log.error("\t--> Unexpected error while decoding Credential Schema: {}", e.getMessage());
            throw new OpenDidException(ErrorCode.DECODING_FAILED);
        }
    }

    /**
     * Extracts the issuer DID from a credential schema ID.
     *
     * The schema ID follows the format: issuer-did:2:schema-name:version
     * where issuer-did is typically in the format: did:method:identifier
     *
     * @param schemaId the credential schema ID to parse
     * @return the issuer DID extracted from the schema ID
     * @throws OpenDidException if schemaId is null, empty, or has invalid format
     *
     * @example
     * Input: "did:example:123:2:MySchema:1.0"
     * Output: "did:example:123"
     */
    private String extractIssuerDidFromCredentialSchemaId(String schemaId) {
        log.debug("\t--> Extracting issuer DID from Credential Schema ID ***");

        if (schemaId == null || schemaId.trim().isEmpty()) {
            log.error("\t--> Invalid schemaId: {}", schemaId);
            throw new OpenDidException(ErrorCode.INVALID_CREDENTIAL_SCHEMA_ID);
        }

        String[] parts = schemaId.split(":");
        if (parts.length < 4) {
            log.error("\t--> Invalid schemaId format: {}", schemaId);
            throw new OpenDidException(ErrorCode.INVALID_CREDENTIAL_SCHEMA_ID);
        }

        // issuer-did는 did:method:identifier 형태여야 함
        if (!"did".equals(parts[0])) {
            log.error("\t--> Invalid issuer DID format in schemaId: {}", schemaId);
            throw new OpenDidException(ErrorCode.INVALID_CREDENTIAL_SCHEMA_ID);
        }

        // did:method:identifier 추출
        String issuerDid = parts[0] + ":" + parts[1] + ":" + parts[2];
        log.debug("\t--> Extracted issuerDid: {}", issuerDid);
        return issuerDid;
    }

    private void validateCredentialSchema(CredentialSchema credentialSchema) {
        Map<String, Object> fieldsToValidate = Map.of(
                "Credential Schema ID", credentialSchema.getId(),
                "Credential Schema name", credentialSchema.getName(),
                "Credential Schema version", credentialSchema.getVersion(),
                "Credential Schema attribute names", credentialSchema.getAttrNames(),
                "Credential Schema tag", credentialSchema.getTag()
        );

        for (Map.Entry<String, Object> entry : fieldsToValidate.entrySet()) {
            if (entry.getValue() == null) {
                log.error("\t--> {} is missing", entry.getKey());
                throw new OpenDidException(ErrorCode.INVALID_CREDENTIAL_SCHEMA);
            }
        }
    }

}