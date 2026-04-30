/*
 * Decompiled with CFR 0.152.
 */
package org.omnione.did.poc.pqc.server.controller;

import com.google.gson.Gson;
import org.omnione.did.poc.pqc.server.ResponseMessage;
import org.omnione.did.poc.pqc.server.util.JsonUtils;
import org.omnione.did.poc.pqc.server.util.MockDataFactory;
import org.omnione.did.data.model.vc.CredentialSchema;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/list/api/v1"})
public class ListController {
    @Value(value="${vc.issuance-mode}")
    private String issuanceModeProp;

    @GetMapping(value={"/allowed-ca/list"})
    public ResponseEntity<String> getAllowedCaList(@RequestParam(value="wallet") String wallet) {
        String response = ResponseMessage.Cas.LIST_ALLOWED_CA;
        return new ResponseEntity<String>(response, (HttpStatusCode)HttpStatus.OK);
    }

    @GetMapping(value={"/vcplan/list"})
    public String getVcPlanList() {
        HashMap<String, Serializable> responseMap = new HashMap<String, Serializable>();
        responseMap.put("count", Integer.valueOf(1));
        CredentialSchema credentialSchema = MockDataFactory.createCredentialSchema(
                ResponseMessage.Common.SERVER_URL + "/issuer/api/v1/vc/vcschema?name=omnione.student_id",
                "OsdSchemaCredential");
        HashMap<String, Boolean> option = new HashMap<String, Boolean>();
        option.put("allowUserInit", true);
        option.put("allowIssuerInit", false);
        option.put("delegatedIssuance", false);
        HashMap<String, Object> vcPlan = new HashMap<String, Object>();
        vcPlan.put("vcPlanId", "student.user-init");
        vcPlan.put("name", "Student ID VC plan");
        vcPlan.put("description", "Student ID VC plan");
        vcPlan.put("tags", Collections.singletonList("student.user-init"));
        vcPlan.put("credentialSchema", credentialSchema);
        vcPlan.put("option", option);
        vcPlan.put("allowedIssuers", Collections.singletonList("did:omn:issuer"));
        vcPlan.put("manager", "did:omn:issuer");
        if ("PROXY".equalsIgnoreCase(this.issuanceModeProp)) {
            vcPlan.put("issuanceMode", (Object)VC_ISSUANCE_MODE.PROXY);
            vcPlan.put("endpoints", Collections.singletonList(ResponseMessage.Common.PROXY_SERVER_URL));
        } else {
            vcPlan.put("issuanceMode", (Object)VC_ISSUANCE_MODE.DIRECT);
        }
        ArrayList<HashMap<String, Object>> items = new ArrayList<HashMap<String, Object>>();
        items.add(vcPlan);
        responseMap.put("items", items);
        Gson gson = JsonUtils.GSON;
        return gson.toJson(responseMap);
    }

    public static enum VC_ISSUANCE_MODE {
        PROXY,
        DIRECT;


        public String getValue() {
            return this.name();
        }
    }
}

