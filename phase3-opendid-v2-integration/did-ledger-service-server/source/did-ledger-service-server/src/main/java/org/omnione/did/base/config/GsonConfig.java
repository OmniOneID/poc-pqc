package org.omnione.did.base.config;

import org.omnione.did.zkp.datamodel.util.GsonWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GsonConfig {
    @Bean
    public GsonWrapper gsonWrapper() {
        return new GsonWrapper();
    }
}
