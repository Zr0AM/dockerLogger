package org.omnomnom.dockerLogger.configuration;

import org.omnomnom.dockerLogger.vault.Secret;
import org.omnomnom.dockerLogger.vault.VaultSecret;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vault.exception.VaultTokenException;

import javax.sql.DataSource;
import java.util.List;

@Component
public class DbConfig {

    private String DB_CONN_STRING;
    private String DB_USERNAME;
    private String DB_PASSWORD;

    public void setProperties(VaultSecret vaultSecret) {
        List<Secret> secrets = vaultSecret.getSecrets();

        String value;
        for (Secret secret : secrets) {
            value = secret.getStaticVersion().toString();

            switch (secret.getName()) {
                case "DB_CONN_STRING":
                    DB_CONN_STRING = value;
                    break;
                case "DB_USERNAME":
                    DB_USERNAME = value;
                    break;
                case "DB_PASSWORD":
                    DB_PASSWORD = value;
                    break;
            }
        }

        if (DB_CONN_STRING == null || DB_USERNAME == null || DB_PASSWORD == null) {
            throw new VaultTokenException("Not all vault secrets have been retrieved");
        }

    }

    @Bean
    public JdbcTemplate dbJdbcTemplate() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        jdbcTemplate.setDataSource(this.dbDataSource());

        return jdbcTemplate;
    }

    private DataSource dbDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:sqlserver://" + DB_CONN_STRING + ";encrypt=true;trustServerCertificate=true")
                .username(DB_USERNAME)
                .password(DB_PASSWORD)
                .driverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
                .build();
    }

}
