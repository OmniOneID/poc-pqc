package org.omnione.did.base.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class OpenApiConfig {
    private final BuildProperties buildProperties;

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(getInfo());
    }

    private Info getInfo() {
        return new Info()
                .title(buildProperties.getName() + " API")
                .description(buildProperties.getName())
                .version(buildProperties.getVersion())
                .license(getLicense());
    }

    private License getLicense() {
        return new License().name("Apache 2.0")
                .url("https://github.com/OmniOneID");
    }

}
