package com.howners.gestion.integration;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** Base CI dédiée uniquement ; chaque test possède un schéma isolé. */
@EnabledIfEnvironmentVariable(named = "HOWNERS_TEST_DATABASE_URL", matches = ".+")
class ProductionMigrationTest {
    @Test void freshProductionContainsNoDemoAccounts() throws Exception {
        inIsolatedSchema(source -> {
            migrate(source, "prod");
            JdbcTemplate jdbc = new JdbcTemplate(source);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email LIKE '%@howners.test'", Long.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM stripe_processed_events", Long.class)).isZero();
        });
    }

    @Test void productionUpgradeDisablesDemoAccountsAndRevokesTokens() throws Exception {
        inIsolatedSchema(source -> {
            migrate(source, "demo");
            JdbcTemplate jdbc = new JdbcTemplate(source);
            Long before = jdbc.queryForObject("SELECT count(*) FROM users WHERE email LIKE '%@howners.test'", Long.class);
            assertThat(before).isPositive();
            migrate(source, "prod");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email LIKE '%@howners.test'", Long.class)).isEqualTo(before);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email LIKE '%@howners.test' AND (enabled OR token_version = 0)", Long.class)).isZero();
        });
    }

    private void migrate(DriverManagerDataSource source, String contexts) throws Exception {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(source);
        liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.xml");
        liquibase.setContexts(contexts);
        liquibase.afterPropertiesSet();
    }

    private void inIsolatedSchema(CheckedTest test) throws Exception {
        String url = System.getenv("HOWNERS_TEST_DATABASE_URL");
        String user = System.getenv("HOWNERS_TEST_DATABASE_USER");
        String password = System.getenv("HOWNERS_TEST_DATABASE_PASSWORD");
        var admin = new JdbcTemplate(new DriverManagerDataSource(url, user, password));
        String schema = "audit_test_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE SCHEMA " + schema);
        try {
            String schemaUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
            test.run(new DriverManagerDataSource(schemaUrl, user, password));
        } finally {
            // Le nom est généré ci-dessus, jamais fourni par l'utilisateur ou une base existante.
            admin.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }
    private interface CheckedTest { void run(DriverManagerDataSource source) throws Exception; }
}
