package org.omnione.did.poc.pqc.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.omnione.did.poc.pqc.server.ResponseMessage;
import org.omnione.did.poc.pqc.server.service.MockService;
import org.omnione.did.poc.pqc.server.util.MockDataFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.omnione.did.crypto.enums.MultiBaseType;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.util.MultiBaseUtils;
import org.omnione.did.data.model.vc.VcMeta;
import org.omnione.did.data.model.vc.VerifiableCredential;
import org.omnione.did.data.model.vc.VcProof;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = {"/api-gateway/api/v1"})
public class ApiController {
    private static final Logger log = LoggerFactory.getLogger(ApiController.class);
    private final MockService mockService;
    private static String getUserDidDataPath() {
        return org.omnione.did.poc.pqc.server.util.MockDataInitializer.getUserDidDataPath();
    }

    public ApiController(MockService mockService) {
        this.mockService = mockService;
    }

    @GetMapping(value = {"/vc-meta"}, produces = {"application/json"})
    public ResponseEntity<?> getVcMeta(@RequestParam(value = "vcId") String vcId) {
        String response = this.getVcMetaInfo(vcId);
        HashMap<String, String> result = new HashMap<>();
        result.put("vcId", vcId);
        try {
            result.put("vcMeta", MultiBaseUtils.encode(response.getBytes(StandardCharsets.UTF_8), MultiBaseType.base64));
        } catch (CryptoException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(result);
    }

    public String getVcMetaInfo(String vcId) {
        VcMeta vcMeta = MockDataFactory.createVcMeta(
                vcId,
                "did:omn:issuer",
                ResponseMessage.Common.SERVER_URL + "/issuer/api/v1/certificate-vc",
                "did:omn:3bXqfaquMudnGfpF2v4rAqsnKEpS",
                "ACTIVE"
        );
        return vcMeta.toJson();
    }

    @GetMapping(value = {"/did-doc"}, produces = {"application/json"})
    public ResponseEntity<?> getDidDoc(@RequestParam(value = "did") String did) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            File file = new File(getUserDidDataPath());
            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User data file not found."));
            }
            List<Map<String, Object>> users = objectMapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> user : users) {
                if (did.equals(user.get("holderDID"))) {
                    String ownerDidDoc = (String) user.get("ownerDidDoc");
                    return ResponseEntity.ok(Map.of("didDoc", ownerDidDoc));
                }
                if (did.equals("did:omn:holder")) {
                    String ownerDidDoc = "z3pjD5vQmWrxrZhPmzx2HDaHNCiWnAgs9AytzRT4DEvrdGs9Rr5Trrbu3XsngUTBZgfKfzPWPLDDFjr9Cw9y4MYTr3SAjnda5gqDToJqrc1X9zdkTAJaqRVN8oSZEz2CzHGH8cucC4AD7xWCCfSYAxeGbDnfBpjK2j1zEBKEEQeVs82wV3VXWYyJfpQUjzSe7tWB6eUz9GffCob5QGSTeYnDp8HvP1CXzb6yXeh1eiYVGtY4nZQ7eS5xxtQeycT7gLRqcSfaunoXDBN63ouUHW3KvbqMGUfp37SBusGS4VT1mksU5mUbSDi2eherDfuqxjTJGfaUsqUbXqeb1RnYR62A8wnmF8sxuUu6pD9QQPJJknsQe2dLH9LUT31EQQuvndjqKsgZCduPzCgSNUjyseQtxLGX5ERMWZJDaE66Q79cu5mAnrKYJ1EEiNBUdVmRKs3Grc3VhENcaTegN8ytwwTvdgqATR1AbnpU2v3BsBKoigZXCskL5ubhDzAJaWZGbgUVaxKXfwh7SxArnjNo6fwUhWMVJLDhuhuLoppysLaXpbqDXjqvo51cQReA8yEQPMh6NMWqRAArZTcUNoJsFZcujASpow4pNgo7RWb8ET9XJVNJtB3kvGNM2vCdnXcY4VEFGKXpvQ6bJCp2qUiDwMkt54tBEaFMwRDpDsaewUq3gzygwNoDsG3oxaPqDmhnPHicAJH77vG2VMXwhPKiQ2QAfYRPBSZWo6FsSHFWuXwYGFa2UCgqMXhbKq1ftxW8Xc7vdaSCdN5W2YcMVSr1iYivQA3Se3G8Q9pjUXpdV8w7MNfyHbzWknnJ1tJJnbfXCZWHKGZWfdGcoRh4SEYCjqgeNmyUJr8MNMFhLXfYCMAmqkyLs3iZAi9SFmsFTAeVuhiG5oQkeHCGG3ZRWHa1zenbYv1NYh9ZXBBdGuxm6f5F4zX3eEUYUrYReAGgfua6DP6YNsQjMfYFCeTk1eEpvP6gA4HjrP6MAFphnM11ZoCYSScMwaQe7QxgKttiCiUxiktGTJh2CkkqBwSduQ8qE6tDU9jj5EC4tiVcJdKuxNNvk3LmHFLTBnQaKbYrTwZ8L9PJV9fKR63x9iCVe8RcnX7qtzFimFqD8wk1RSf9q87RVNYyXMqZAyyqR4dgfio3dR4zGhuEzSc71RRhzSN5SWWDKo1423CAbatLS6b4M3focnQbicXcpEuATzDBWB3AC";
                    return ResponseEntity.ok(Map.of("didDoc", ownerDidDoc));
                }
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "DID not found."));
        } catch (IOException e) {
            log.error("Error reading user data file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error processing request."));
        }
    }

    @GetMapping(value = {"/certificate-vc"})
    public ResponseEntity<String> getCertVc() {
        String response = this.getCerificateVC();
        return ResponseEntity.ok(response);
    }

    public String getCerificateVC() {
        Map<String, String> attribute = new HashMap<>();
        attribute.put("licenseNumber", "1234567890");

        VerifiableCredential vc = MockDataFactory.createCertificateVcWithAttribute(
                "did:omn:cas", "o=cas", "AppProvider",
                "did:omn:tas", "tas", "did:omn:tas?versionId=1#assert",
                attribute
        );

        VcProof proof = (VcProof) vc.getProof();
        proof.setProofValueList(this.mockService.generateProofValueList(vc));

        return this.mockService.signVc(vc);
    }
}
