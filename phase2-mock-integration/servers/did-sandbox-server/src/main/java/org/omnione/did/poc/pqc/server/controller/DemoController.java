/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.omnione.did.poc.pqc.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.zxing.WriterException;
import org.omnione.did.poc.pqc.server.util.JsonUtils;
import org.omnione.did.poc.pqc.server.util.MockDataFactory;
import org.omnione.did.poc.pqc.server.ResponseMessage;
import org.omnione.did.poc.pqc.server.dto.ai.VpResultDto;
import org.omnione.did.poc.pqc.server.util.DataStore;
import org.omnione.did.poc.pqc.server.util.QrMaker;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.Generated;
import org.omnione.did.common.vo.QrImageData;
import org.omnione.did.crypto.enums.MultiBaseType;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping(value={"/demo/api/v1"})
public class DemoController {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(DemoController.class);
    private final DataStore dataStore;

    @PostMapping(value={"/qr-data/generate-vp-qr"})
    public Map<String, Object> generateVpQrData(@RequestBody Map<String, String> data) {
        return this.vpOfferRefresh(data.get("ipAddress"), data.get("apiKey"), data.get("policyId"));
    }

    @PostMapping(value={"/qr-data/generate-edoc-qr"})
    public Map<String, Object> generateEdocQrData(@RequestBody Map<String, String> data) throws IOException, WriterException {
        return this.submitEdoc(data.get("studentId"), data.get("url"), data.get("transcriptType"));
    }

    public Map<String, Object> submitEdoc(String studentId, String url, String transcriptType) throws IOException, WriterException {
        log.debug("=== Starting submitEdoc ===");
        HashMap<String, Object> payload = new HashMap<String, Object>();
        payload.put("url", url + "/demo/api/v1/edoc/submit");
        payload.put("authToken", Base64.getUrlEncoder().withoutPadding().encodeToString("ai-auth-token".getBytes()));
        payload.put("transcriptType", transcriptType);
        payload.put("studentId", studentId);
        Gson gson = JsonUtils.GSON;
        String encodedPayload = null;
        try {
            encodedPayload = MultiBaseUtils.encode(gson.toJson(payload).getBytes(StandardCharsets.UTF_8), MultiBaseType.base64);
        }
        catch (CryptoException e) {
            throw new RuntimeException(e);
        }
        VpResultDto vpResultDto = VpResultDto.builder().institution("AI Portal").payloadType("SUBMIT_EDOC").payload(encodedPayload).validUntil(MockDataFactory.nowPlusOneYear()).build();
        log.debug("\t Make QR Image");
        QrImageData qrImageData = QrMaker.makeQrImage(vpResultDto);
        HashMap<String, Object> response = new HashMap<String, Object>();
        response.put("qrImage", qrImageData.getQrIamge());
        response.put("qrData", vpResultDto);
        log.debug("=== End submitEdoc ===");
        return response;
    }

    public Map<String, Object> vpOfferRefresh(String ipAddress, String apiKey, String policyId) {
        log.debug("=== Starting VpOffer ===");
        log.debug("\t Make VP Offer Request");
        String offerId = "d2076247-44aa-4a5e-b94a-6bb283adbe03";
        try {
            Map payload = new HashMap<String, Object>();
            if (ipAddress != null && !ipAddress.isEmpty()) {
                RestTemplate restTemplate = new RestTemplate();
                String url = "http://" + ipAddress + "/verifier/api/v1/request-offer";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-API-Key", apiKey);
                HashMap<String, String> requestBody = new HashMap<String, String>();
                requestBody.put("id", "202303241738241234561234ABCD");
                requestBody.put("policyId", "4fae00a5-3d15-4cf9-a8c4-42f9eeced4f7");
                if (policyId != null && !policyId.isEmpty()) {
                    requestBody.put("policyId", policyId);
                }
                HttpEntity entity = new HttpEntity(requestBody, headers);
                Map responseOffer = (Map)restTemplate.exchange(url, HttpMethod.POST, entity, Map.class, new Object[0]).getBody();
                payload = (Map)responseOffer.get("payload");
                offerId = (String)payload.get("offerId");
            } else {
                payload.put("offerId", offerId);
                payload.put("type", "VerifyOffer");
                payload.put("mode", "Direct");
                payload.put("device", "WEB");
                payload.put("service", "StudentID.vc");
                payload.put("endpoints", Collections.singletonList(ResponseMessage.Common.SERVER_URL + "/verifier/api/v1/request-verify"));
                payload.put("validUntil", MockDataFactory.nowPlusOneYear());
                payload.put("locked", false);
            }
            Gson gson = JsonUtils.GSON;
            String encodedPayload = null;
            try {
                encodedPayload = MultiBaseUtils.encode(gson.toJson(payload).getBytes(StandardCharsets.UTF_8), MultiBaseType.base64);
            }
            catch (CryptoException e) {
                throw new RuntimeException(e);
            }
            VpResultDto vpResultDto = VpResultDto.builder().institution("AI Portal").payloadType("SUBMIT_VP").payload(encodedPayload).validUntil(MockDataFactory.nowPlusOneYear()).build();
            log.debug("\t Make QR Image");
            QrImageData qrImageData = QrMaker.makeQrImage(vpResultDto);
            HashMap<String, Object> response = new HashMap<String, Object>();
            response.put("qrImage", qrImageData.getQrIamge());
            response.put("qrData", vpResultDto);
            response.put("offerId", offerId);
            response.put("validUntil", vpResultDto.getValidUntil());
            log.debug("=== End VpOfferRefresh ===");
            return response;
        }
        catch (Exception e) {
            log.error("Exception occurred during Requesting VP Offer: {}", (Object)e.getMessage(), (Object)e);
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value={"/vp-result"})
    public String vpResult(@RequestBody Map<String, String> data) {
        log.debug("=== Starting VP confirm ===");
        String ipAddress = data.get("ipAddress");
        String vpOfferId = data.get("vpOfferId");
        String apiKey = data.get("apiKey");
        if (ipAddress != null && !ipAddress.isEmpty()) {
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://" + ipAddress + "/verifier/api/v1/confirm-verify";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);
            HashMap<String, String> requestBody = new HashMap<String, String>();
            requestBody.put("offerId", vpOfferId);
            HttpEntity entity = new HttpEntity(requestBody, headers);
            String reposnse = (String)restTemplate.exchange(url, HttpMethod.POST, entity, String.class, new Object[0]).getBody();
            return reposnse;
        }
        return ResponseMessage.Session.CLAIM;
    }

    @PostMapping(value={"/edoc/submit"})
    public Map<String, Object> submit(@RequestBody Map<String, String> data) throws IOException, WriterException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String request = objectMapper.writeValueAsString(data);
            this.dataStore.save(data.get("studentId"), data.get("transcript"));
            if (data.get("transcript") == null || data.get("transcript").isEmpty()) {
                throw new IllegalArgumentException("Certificate data is missing.");
            }
            HashMap<String, Object> response = new HashMap<String, Object>();
            response.put("result", true);
            return response;
        }
        catch (Exception e) {
            log.error("Certificate submission failed: {}", (Object)e.getMessage());
            HashMap<String, Object> response = new HashMap<String, Object>();
            response.put("result", false);
            response.put("message", "Certificate submission failed. (" + e.getMessage() + ")");
            return response;
        }
    }

    @GetMapping(value={"/edoc/confirm"})
    public Map<String, Object> confirm(@RequestParam(value="studentId") String studentId, @RequestParam(value="authToken") String authToken) throws IOException, WriterException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String certificate = this.dataStore.consume(studentId);
            if (certificate == null || certificate.isEmpty()) {
                throw new IllegalArgumentException("Certificate data is empty.");
            }
            HashMap<String, Object> response = new HashMap<String, Object>();
            response.put("result", true);
            response.put("certificate", certificate);
            return response;
        }
        catch (Exception e) {
            log.error("Certificate submission verification failed: {}", (Object)e.getMessage());
            HashMap<String, Object> response = new HashMap<String, Object>();
            response.put("result", false);
            response.put("message", "Certificate submission verification failed.");
            return response;
        }
    }

    @PostMapping(value={"/qr-data/generate_raw"})
    public ResponseEntity<String> generateQrData(@RequestBody String data) {
        return new ResponseEntity<String>(this.qrData(), (HttpStatusCode)HttpStatus.OK);
    }

    private String qrData() {
        HashMap<String, Object> payload = new HashMap<String, Object>();
        payload.put("offerId", "d2076247-44aa-4a5e-b94a-6bb283adbe03");
        payload.put("type", "VerifyOffer");
        payload.put("mode", "Direct");
        payload.put("device", "WEB");
        payload.put("service", "StudentID.vc");
        payload.put("endpoints", Collections.singletonList(ResponseMessage.Common.SERVER_URL + "/verifier/api/v1/request-verify"));
        payload.put("validUntil", MockDataFactory.nowPlusOneYear());
        payload.put("locked", false);
        Gson gson = JsonUtils.GSON;
        String encodedPayload = null;
        try {
            encodedPayload = MultiBaseUtils.encode(gson.toJson(payload).getBytes(StandardCharsets.UTF_8), MultiBaseType.base64);
        }
        catch (CryptoException e) {
            throw new RuntimeException(e);
        }
        HashMap<String, String> qrData = new HashMap<String, String>();
        qrData.put("payload", encodedPayload);
        qrData.put("payloadType", "SUBMIT_VP");
        qrData.put("validUntil", MockDataFactory.nowPlusOneYear());
        qrData.put("institution", "AI Portal");
        return gson.toJson(qrData);
    }

    @Generated
    public DemoController(DataStore dataStore) {
        this.dataStore = dataStore;
    }
}

