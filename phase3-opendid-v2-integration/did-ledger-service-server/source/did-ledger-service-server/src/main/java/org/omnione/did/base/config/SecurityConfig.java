package org.omnione.did.base.config;

import org.omnione.did.base.constants.UrlConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@RequiredArgsConstructor
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    static {
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity)
            throws Exception {

        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(this::authorizeHttpRequestsCustomizer)
                .build();
    }

    private void authorizeHttpRequestsCustomizer(AuthorizeHttpRequestsConfigurer<HttpSecurity>
                                                         .AuthorizationManagerRequestMatcherRegistry configurer) {
        allowedUrlsConfigurer(configurer);
        configurer.anyRequest().permitAll();
    }

    private void allowedUrlsConfigurer(AuthorizeHttpRequestsConfigurer<HttpSecurity>
                                               .AuthorizationManagerRequestMatcherRegistry configurer) {
        // Allowed API
//        configurer
//            .requestMatchers(HttpMethod.GET, UrlConstant.Repository.V1)
//                .permitAll();
    }

}