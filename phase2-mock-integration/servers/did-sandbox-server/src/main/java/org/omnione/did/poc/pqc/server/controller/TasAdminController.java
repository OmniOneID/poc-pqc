package org.omnione.did.poc.pqc.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.omnione.did.poc.pqc.server.util.MockDataInitializer;
import org.omnione.did.crypto.util.MultiBaseUtils;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * TAS Admin 엔드포인트.
 * enrollment SDK의 registerDidToTas()에 대응.
 * 경로: /tas/admin/v1/...
 */
@RestController
@RequestMapping(value = {"/tas/admin/v1"})
public class TasAdminController {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(TasAdminController.class);

    /**
     * Entity DID 등록 (공개 엔드포인트).
     * enrollment SDK에서 registerDidToTas() 호출 시 도달.
     *
     * Request body:
     *   { "didDoc": "z...(multibase encoded)", "name": "EntityName",
     *     "serverUrl": "http://...", "certificateUrl": "http://...", "role": "TAS" }
     *
     * 수신한 DID document를 user_did 파일에 저장한다.
     */
    @PostMapping(value = {"/entities/register-did/public"})
    public String registerDidPublic(@RequestBody String request) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> requestMap = objectMapper.readValue(request, new TypeReference<>() {});

            String encodedDidDoc = (String) requestMap.get("didDoc");
            String name = (String) requestMap.get("name");
            String role = (String) requestMap.get("role");

            // didDoc 디코딩하여 DID 추출
            byte[] decodedBytes = MultiBaseUtils.decode(encodedDidDoc);
            String didDocJson = new String(decodedBytes, StandardCharsets.UTF_8);
            Map<String, Object> didDoc = objectMapper.readValue(didDocJson, new TypeReference<>() {});
            String did = (String) didDoc.get("id");

            // user_did 파일에 저장
            //saveToUserDidFile(did, encodedDidDoc, objectMapper);

            return "{}";
        } catch (Exception e) {
            log.error("Failed to register entity DID", e);
            throw new RuntimeException(e);
        }
    }

    private void saveToUserDidFile(String did, String ownerDidDoc, ObjectMapper mapper) throws IOException {
        File file = new File(MockDataInitializer.getUserDidDataPath());
        file.getParentFile().mkdirs();

        List<Map<String, Object>> users;
        if (file.exists() && file.length() > 2) {
            users = mapper.readValue(file, new TypeReference<>() {});
        } else {
            users = new ArrayList<>();
        }

        // 기존 항목 갱신 또는 신규 추가
        boolean found = false;
        for (Map<String, Object> user : users) {
            if (did.equals(user.get("holderDID"))) {
                user.put("ownerDidDoc", ownerDidDoc);
                found = true;
                break;
            }
        }
        if (!found) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("holderDID", did);
            entry.put("ownerDidDoc", ownerDidDoc);
            users.add(entry);
        }

        mapper.writeValue(file, users);
    }
}
