package vault.configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.omnomnom.dockerlogger.util.Converter;
import org.omnomnom.dockerlogger.vault.VaultToken;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import vault.exception.VaultTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VaultConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(VaultConfig.class);

    @Resource
    RestTemplate restTemplate;

    @Value("${vault-env}")
    String vaultEnv;

    @Value("${vault-client}")
    String clientId;

    @Value("${vault-secret}")
    String clientSecret;

    @Value("${spring.cloud.vault.uri}")
    String vaultUri;

    @Value("${spring.cloud.vault.api.base}")
    String apiBase;

    @Value("${spring.cloud.vault.api.org}")
    String apiOrg;

    @Value("${spring.cloud.vault.api.proj}")
    String apiProj;

    private VaultToken vaultToken;

    private URI apiUrl;

    private final Map<String, String> vaultSecrets = new ConcurrentHashMap<>();

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
                apiBase + apiOrg + "/projects/" + apiProj + "/apps/" + vaultEnv + "/secrets:open"
        );
    }

    private String getToken() {
        if (vaultToken == null) {
            LOGGER.warn("Vault token is null, attempting to refresh.");
            refreshToken();
        } else if (LocalDateTime.now().plusMinutes(5).toInstant(ZoneOffset.UTC).isAfter(vaultToken.getExpiresBy())) {
            LOGGER.info("Vault token expiring soon or expired, refreshing.");
            refreshToken();
        }

        if (vaultToken == null || !vaultToken.getValid()) {
            throw new VaultTokenException("Cannot proceed without a valid Vault token.");
        }

        return vaultToken.getAccessToken();
    }

    private void refreshToken() {
        LOGGER.info("Refreshing Vault Token");

        Map<String, Object> body = new HashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("grant_type", "client_credentials");
        body.put("audience", "audience=https://api.hashicorp.cloud"); //TODO confirm this is needed

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/json");

        RequestEntity<Map<String, Object>> request = new RequestEntity<>(
                body, headers, HttpMethod.POST, URI.create(vaultUri));

        try {
            ResponseEntity<String> response = restTemplate.exchange(request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                LOGGER.error("Bad response received from Vault token endpoint {}: {}", response.getStatusCode(), response.getBody());
                throw new VaultTokenException("Bad response received from Vault " + response.getStatusCode() + ": " + response.getBody());
            }

            this.vaultToken = convertRespBody(response.getBody());

            if (!vaultToken.getValid()) {
                LOGGER.error("Could not obtain a valid vault token after successful HTTP call.");
                throw new VaultTokenException("Could not obtain a valid vault token");
            }
            LOGGER.info("Successfully refreshed Vault Token.");

        } catch (Exception e) {
            LOGGER.error("Error refreshing Vault token: {}", e.getMessage(), e);
            throw new VaultTokenException("Error refreshing Vault token " + e);
        }

    }

    private VaultToken convertRespBody(String respBody) {
        VaultToken vaultToken = new VaultToken();
        vaultToken.setValid(false);

        try {
            JSONObject json = new JSONObject(respBody);

            vaultToken.setAccessToken(json.getString("access_token"));
            vaultToken.setTokenType(json.getString("token_type"));

            long expiresInSeconds = json.getLong("expires_in");
            vaultToken.setExpiresBy(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(expiresInSeconds).toInstant(ZoneOffset.UTC));
            vaultToken.setValid(true);
            LOGGER.info("Converted response body to VaultToken. Expires in {} seconds.", expiresInSeconds);

        } catch (Exception e) {
            LOGGER.error("Could not convert response body to VaultToken", e);
            return new VaultToken();

        }
        return vaultToken;
    }

    private void getSecrets() {
        LOGGER.info("Attempting to get Vault Secrets from: {}", apiUrl);
        this.vaultSecrets.clear(); // Clear previous secrets before fetching new ones

        try {
            String token = getToken(); // Ensures token is valid and refreshed if needed

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Authorization", "Bearer " + token);
            headers.add("Accept", "application/json");

            RequestEntity<String> request = new RequestEntity<>(headers, HttpMethod.GET, apiUrl);
            ResponseEntity<String> response = restTemplate.exchange(request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                LOGGER.error("Bad response received from Vault secrets endpoint {}: {}", response.getStatusCode(), response.getBody());
                throw new VaultTokenException("Bad response received from Vault secrets endpoint " + response.getStatusCode() + ": " + response.getBody());
            }

            JSONObject json = Converter.getJsonObjectFromString(response.getBody());
            if (json == null) {
                throw new VaultTokenException("Could not convert secrets response body to JSONObject");
            }

            JSONArray secretsArray = json.optJSONArray("secrets");
            if (secretsArray == null) {
                LOGGER.warn("No 'secrets' array found in the Vault response.");
                return; // Nothing to process
            }

            LOGGER.info("Retrieved {} secrets from Vault. Parsing...", secretsArray.length());
            for (int i = 0; i < secretsArray.length(); i++) {
                JSONObject secretObj = secretsArray.getJSONObject(i);
                String secretName = secretObj.optString("name", null);

                // Navigate nested structure based on VaultSecret.json
                JSONObject versionObj = secretObj.optJSONObject("latest_version");
                if (versionObj == null) {
                    versionObj = secretObj.optJSONObject("static_version");
                }

                String secretValue = null;
                if (versionObj != null) {
                    secretValue = versionObj.optString("value", null);
                }

                if (secretName != null && secretValue != null) {
                    this.vaultSecrets.put(secretName, secretValue);
                } else {
                    LOGGER.warn("Skipping secret at index {} due to missing name or value.", i);
                }
            }
            LOGGER.info("Finished parsing Vault secrets. Stored {} secrets.", this.vaultSecrets.size());

        } catch (Exception e) {
            LOGGER.error("Error getting Vault secrets: {}", e.getMessage(), e);
            throw new VaultTokenException("Failed to retrieve secrets from Vault" + e);
        }
    }

    public Map<String, String> getVaultSecrets() {
        return Collections.unmodifiableMap(this.vaultSecrets);
    }

}
