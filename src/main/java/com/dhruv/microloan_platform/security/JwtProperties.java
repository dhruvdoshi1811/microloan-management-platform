package com.dhruv.microloan_platform.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Binds the {@code jwt.*} properties (see application.properties). */
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    /** HMAC signing secret. Must be long enough for HS256 (>= 32 bytes). */
    private String secret;

    private long expirationMs;
}
