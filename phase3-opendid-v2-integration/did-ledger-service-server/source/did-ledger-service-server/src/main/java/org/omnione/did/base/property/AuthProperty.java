package org.omnione.did.base.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth")
public class AuthProperty {
    private static Token token = new Token();

    @Getter @Setter
    public static class Token {
        private boolean enable = true;
    }
}
