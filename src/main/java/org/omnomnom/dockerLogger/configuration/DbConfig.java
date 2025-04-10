package org.omnomnom.dockerLogger.configuration;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import vault.configuration.VaultConfig;
import javax.sql.DataSource;
import java.util.Map;

@Configuration
@DependsOn("vaultConfig")
public class DbConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DbConfig.class);

    @Resource
    private VaultConfig vaultConfig;

    @Bean
    public DataSource dbDataSource() {
        LOGGER.info("Attempting to configure dbDataSource bean using secrets from VaultConfig...");

        Map<String, String> secrets = vaultConfig.getVaultSecrets();

        String connectionString = secrets.get("DB_CONN_STRING");
        String username = secrets.get("DB_USER_NAME");
        String password = secrets.get("DB_PASSWORD");

        // Check if secrets were successfully retrieved
        if (connectionString == null || username == null || password == null) {
            LOGGER.error("Required database configuration properties (database.connection.string, database.username, database.password) " +
                    "were not found in the secrets retrieved by VaultConfig. " +
                    "Check VaultConfig logs and ensure secrets exist in Vault with the correct names.");
            throw new IllegalStateException("Required database configuration properties not loaded via VaultConfig.");
        } else {
            LOGGER.info("Database properties retrieved from VaultConfig.");
        }

        String jdbcUrl = "jdbc:sqlserver://" + connectionString + ";encrypt=true;trustServerCertificate=true";

        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .driverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
                .build();
    }

    @Bean
    public JdbcTemplate dbJdbcTemplate(DataSource dbDataSource) {
        LOGGER.info("Setting up dbJdbcTemplate bean using the dbDataSource");
        return new JdbcTemplate(dbDataSource);
    }

}
