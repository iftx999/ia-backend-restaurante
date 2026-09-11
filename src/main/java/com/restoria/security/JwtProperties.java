package com.restoria.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Mapeia o bloco `restoria.security` do application.yml.
 * O secret nunca tem default hardcoded: vem so de JWT_SECRET (ver CLAUDE.md).
 */
@ConfigurationProperties(prefix = "restoria.security")
public record JwtProperties(
        String jwtSecret,
        @DefaultValue("480") int jwtExpiracaoMinutos
) {
}
