package org.omnomnom.dockerLogger;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.omnomnom.dockerLogger.configuration.DbConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.vault.repository.support.VaultRepositoryFactoryBean;
import vault.configuration.VaultConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableJpaRepositories(basePackageClasses = DbConfig.class)
@EnableScheduling
@EnableSchedulerLock(defaultLockAtLeastFor = "PT5M", defaultLockAtMostFor = "PT5M")
public class DockerLoggerApplication {

    @Value("${spring.cloud.vault.uri}")
    private String vaultUri;

    public static void main(String[] args) {
        SpringApplication.run(DockerLoggerApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() { return new RestTemplate(); }

    @Bean
    public VaultTemplate vaultTemplate() {
        return new VaultTemplate(VaultEndpoint.from(vaultUri));
    }

    @Bean
    public VaultConfig vaultConfig() {
        return new VaultConfig();
    }

    @Bean
    public DbConfig dbConfig() {
        return new DbConfig();
    }

}
