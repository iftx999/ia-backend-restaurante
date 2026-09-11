package com.restoria.conhecimento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Tenta habilitar a extensao `pgvector` no Postgres no startup da aplicacao
 * (`CREATE EXTENSION IF NOT EXISTS vector`). Isso e um pre-requisito de
 * infraestrutura (pacote de sistema `postgresql-XX-pgvector`) que este runner
 * nao instala — ele so tenta habilitar a extensao caso ja esteja disponivel
 * no servidor.
 *
 * Desativado no perfil de teste ("test"), ja que os testes rodam contra H2
 * (sem suporte a pgvector).
 */
@Component
@Profile("!test")
public class PgVectorExtensionInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PgVectorExtensionInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public PgVectorExtensionInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        } catch (Exception e) {
            log.warn("Extensao pgvector nao disponivel no Postgres — funcionalidades de RAG "
                    + "(base de conhecimento) nao vao funcionar ate ela ser instalada. "
                    + "Veja docs/04-roadmap.md. Detalhe: {}", e.getMessage());
        }
    }
}
