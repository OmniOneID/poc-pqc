/*
 * Decompiled with CFR 0.152.
 */
package org.omnione.did.poc.pqc.server.config;

import org.omnione.did.poc.pqc.server.ResponseMessage;
import org.omnione.did.poc.pqc.server.config.PqcConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Value(value="${vc.proxy-port:8093}")
    private String proxyPort;

    // PqcConfig를 주입받아 @PostConstruct 순서를 보장
    private final PqcConfig pqcConfig;

    public AppConfig(PqcConfig pqcConfig) {
        this.pqcConfig = pqcConfig;
    }

    @PostConstruct
    public void init() {
        ResponseMessage.Common.setProxyPort(this.proxyPort);
        // PQC 설정에 따라 키 자료 초기화 (PqcConfig.init() 이후 실행 보장)
        ResponseMessage.Crypto.initKeys(PqcConfig.isMlDsa44());
        // ML-KEM-768 키교환 키 초기화
        ResponseMessage.Crypto.initKaKeys(PqcConfig.isMlKem768());
        if (PqcConfig.isMlKem768()) {
            log.info("ML-KEM-768 keys initialized. ENCAP_KEY prefix: {}...",
                    ResponseMessage.Crypto.MLKEM_ENCAP_KEY.substring(0, Math.min(40, ResponseMessage.Crypto.MLKEM_ENCAP_KEY.length())));
            try {
                byte[] decapBytes = org.omnione.did.crypto.util.MultiBaseUtils.decode(ResponseMessage.Crypto.MLKEM_DECAP_KEY);
                byte[] encapBytes = org.omnione.did.crypto.util.MultiBaseUtils.decode(ResponseMessage.Crypto.MLKEM_ENCAP_KEY);
                log.info("ML-KEM-768 decapKey size: {} bytes, encapKey size: {} bytes", decapBytes.length, encapBytes.length);
            } catch (Exception e) {
                log.error("Failed to decode ML-KEM keys for logging", e);
            }
        }
        // 정적 모킹 데이터 재생성
        org.omnione.did.poc.pqc.server.util.MockDataInitializer.initMockData();
    }
}

