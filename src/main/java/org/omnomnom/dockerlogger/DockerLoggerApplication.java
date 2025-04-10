package org.omnomnom.dockerlogger;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import vault.configuration.VaultConfig;

@SpringBootApplication
@EnableJpaRepositories
@EnableScheduling
@EnableSchedulerLock(defaultLockAtLeastFor = "PT5M", defaultLockAtMostFor = "PT5M")
public class DockerLoggerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DockerLoggerApplication.class, args);
    }

    @Bean
    public VaultConfig vaultConfig() {
        return new VaultConfig();
    }

    @Bean
    public RestTemplate restTemplate() { return new RestTemplate(); }

}
