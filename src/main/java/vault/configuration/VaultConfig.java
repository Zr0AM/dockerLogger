package vault.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.omnomnom.dockerLogger.vault.VaultToken;
import org.omnomnom.dockerLogger.configuration.DbConfig;
import org.omnomnom.dockerLogger.vault.VaultSecret;
import vault.exception.VaultTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Component
public class VaultConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(VaultConfig.class);

    private static final String ENV = "DEV";

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    DbConfig dbConfig;

    @Value("${spring.cloud.vault.client_id}")
    private String clientId;

    @Value("${spring.cloud.vault.client_secret}")
    private String clientSecret;

    @Value("${spring.cloud.vault.uri}")
    private String vaultUri;

    @Value("${spring.cloud.vault.api.base}")
    private String apiBase;

    @Value("${spring.cloud.vault.api.org}")
    private String apiOrg;

    @Value("${spring.cloud.vault.api.proj}")
    private String apiProj;

    private VaultToken vaultToken;

    private URI apiUrl;

    @PostConstruct
    public void init() {
        generateUri();
        refreshToken();
        getSecrets();
    }

    @PreDestroy
    public void destroy() {
        this.vaultToken = null;
    }

    private void generateUri() {
        apiUrl = URI.create(
                apiBase + apiOrg + "/projects/" + apiProj + "/apps/" + ENV + "/secrets:open"
        );
    }

    private String getToken() {
        if (vaultToken == null) {
            refreshToken();
        } else if (LocalDateTime.now().plusMinutes(5).toInstant(ZoneOffset.UTC).isAfter(vaultToken.getExpiresBy())) {
            refreshToken();
        }

        return vaultToken.getAccessToken();
    }

    private void refreshToken() {
        LOGGER.info("Refreshing Vault Token");

        Map<String, Object> body = new HashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("grant_type", "client_credentials");
        body.put("audience", "audience=https://api.hashicorp.cloud");

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/json");

        RequestEntity<String> request = new RequestEntity(body, headers, HttpMethod.POST, URI.create(vaultUri));
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new VaultTokenException("Bad response received from Vault " + response.getStatusCode() + ": " + response.getBody());
        }

        this.vaultToken = convertRespBody(response.getBody());

        if (!vaultToken.getValid()) {
            throw new VaultTokenException("Could not obtain a vault token");
        }

    }

    private VaultToken convertRespBody(String respBody) {
        VaultToken vaultToken = new VaultToken();
        vaultToken.setValid(false);

        try {
            JSONObject json = new JSONObject(respBody);

            vaultToken.setAccessToken(json.getString("access_token"));
            vaultToken.setTokenType(json.getString("token_type"));
            vaultToken.setExpiresBy(LocalDateTime.now().plusSeconds(json.getLong("expires_in")).toInstant(ZoneOffset.UTC));
            vaultToken.setValid(true);

        } catch (Exception e) {
            LOGGER.error("Could not convert response body to VaultToken", e);
            return new VaultToken();

        }
        return vaultToken;
    }

    private void getSecrets() {
        LOGGER.info("Getting Vault Secrets");

        String token = getToken();

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/json");
        headers.add("Authorization", "Basic " + token);

        RequestEntity<String> request = new RequestEntity(new HashMap<>(), headers, HttpMethod.POST, apiUrl);
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);


        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new VaultTokenException("Bad response received from Vault " + response.getStatusCode() + ": " + response.getBody());
        }

        VaultSecret vaultSecret = convertSecret(response.getBody());

        dbConfig.setProperties(vaultSecret);

        LOGGER.info("Retrieved Vault Secrets");
    }

    private VaultSecret convertSecret(String responseBody) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(responseBody, VaultSecret.class);
        } catch (Exception e) {
            throw new VaultTokenException("Could not convert response body");
        }
    }

}
