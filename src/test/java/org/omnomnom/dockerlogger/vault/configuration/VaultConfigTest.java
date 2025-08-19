package org.omnomnom.dockerlogger.vault.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omnomnom.dockerlogger.vault.Vaulttoken;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import vault.configuration.VaultConfig;
import vault.exception.VaultTokenException;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Profile("!test")
@ExtendWith(MockitoExtension.class)
class VaultConfigTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private VaultConfig vaultConfig;

    @Captor
    private ArgumentCaptor<RequestEntity<?>> requestEntityCaptor;

    private final String vaultUri = "https://auth.hashicorp.cloud/oauth/token";
    private final String apiBase = "https://api.hashicorp.cloud/vault/v2/organizations/";
    private final String apiOrg = "test-org";
    private final String apiProj = "test-proj";
    private final String appId = "test-app";

    @BeforeEach
    void setUp() {
        // Use ReflectionTestUtils to set the @Value fields, simulating Spring's dependency injection
        ReflectionTestUtils.setField(vaultConfig, "appId", appId);
        ReflectionTestUtils.setField(vaultConfig, "clientId", "test-client-id");
        ReflectionTestUtils.setField(vaultConfig, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(vaultConfig, "vaultUri", vaultUri);
        ReflectionTestUtils.setField(vaultConfig, "apiBase", apiBase);
        ReflectionTestUtils.setField(vaultConfig, "apiOrg", apiOrg);
        ReflectionTestUtils.setField(vaultConfig, "apiProj", apiProj);
    }

    private String getExpectedApiUrl() {
        return apiBase + apiOrg + "/projects/" + apiProj + "/apps/" + appId + "/secrets:open";
    }

    private ResponseEntity<String> createMockTokenResponse(String accessToken, long expiresIn) {
        String responseBody = String.format(
                "{\"access_token\":\"%s\",\"expires_in\":%d,\"token_type\":\"Bearer\"}",
                accessToken, expiresIn
        );
        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    private ResponseEntity<String> createMockSecretsResponse() {
        String responseBody = "{\n" +
                "  \"secrets\": [\n" +
                "    {\n" +
                "      \"name\": \"DB_PASSWORD\",\n" +
                "      \"latest_version\": {\n" +
                "        \"version\": \"1\",\n" +
                "        \"type\": \"kv\",\n" +
                "        \"value\": \"supersecretpassword\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"API_KEY\",\n" +
                "      \"latest_version\": {\n" +
                "        \"version\": \"2\",\n" +
                "        \"type\": \"kv\",\n" +
                "        \"value\": \"abcdef123456\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    @Test
    @DisplayName("init() - Happy Path - Should Refresh Token and Fetch Secrets Successfully")
    void init_happyPath_shouldInitializeSuccessfully() {
        // Arrange
        when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                .thenReturn(createMockTokenResponse("test-token", 3600))
                .thenReturn(createMockSecretsResponse());

        // Act
        vaultConfig.init();

        // Assert
        Map<String, String> secrets = vaultConfig.getVaultSecrets();
        assertNotNull(secrets);
        assertEquals(2, secrets.size());
        assertEquals("supersecretpassword", secrets.get("DB_PASSWORD"));
        assertEquals("abcdef123456", secrets.get("API_KEY"));

        // Verify that an unmodifiable map is returned
        assertThrows(UnsupportedOperationException.class, () -> secrets.put("new", "value"));

        // Verify restTemplate calls
        verify(restTemplate, times(2)).exchange(requestEntityCaptor.capture(), eq(String.class));

        // Verify token request
        RequestEntity<?> tokenRequest = requestEntityCaptor.getAllValues().get(0);
        assertEquals(HttpMethod.POST, tokenRequest.getMethod());
        assertEquals(URI.create(vaultUri), tokenRequest.getUrl());

        // Verify secrets request
        RequestEntity<?> secretsRequest = requestEntityCaptor.getAllValues().get(1);
        assertEquals(HttpMethod.GET, secretsRequest.getMethod());
        assertEquals(URI.create(getExpectedApiUrl()), secretsRequest.getUrl());
        assertEquals("Bearer test-token", secretsRequest.getHeaders().getFirst("Authorization"));
    }

    @Test
    @DisplayName("init() - Token Refresh Fails with HTTP Error - Should Throw VaultTokenException")
    void init_tokenRefreshFails_shouldThrowVaultTokenException() {
        // Arrange
        when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED));

        // Act & Assert
        VaultTokenException exception = assertThrows(VaultTokenException.class, () -> vaultConfig.init());
        assertTrue(exception.getMessage().contains("Bad response received from Vault 401 UNAUTHORIZED"));
        verify(restTemplate, times(1)).exchange(any(RequestEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("init() - Token Refresh Throws Network Exception - Should Throw VaultTokenException")
    void init_tokenRefreshThrowsException_shouldThrowVaultTokenException() {
        // Arrange
        when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

        // Act & Assert
        VaultTokenException exception = assertThrows(VaultTokenException.class, () -> vaultConfig.init());
        assertTrue(exception.getMessage().contains("Error refreshing Vault token"));
        verify(restTemplate, times(1)).exchange(any(RequestEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("init() - Get Secrets Fails - Should Throw VaultTokenException")
    void init_getSecretsFails_shouldThrowVaultTokenException() {
        // Arrange
        when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                .thenReturn(createMockTokenResponse("test-token", 3600))
                .thenReturn(new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND));

        // Act & Assert
        VaultTokenException exception = assertThrows(VaultTokenException.class, () -> vaultConfig.init());
        assertTrue(exception.getMessage().contains("Bad response received from Vault secrets endpoint 404 NOT_FOUND"));
        verify(restTemplate, times(2)).exchange(any(RequestEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("getToken() - Token is Expiring Soon - Should Refresh Token")
    void getToken_whenTokenIsExpiring_shouldRefreshToken() {
        // Arrange: Create a token that expires in 4 minutes (less than the 5-minute threshold)
        Vaulttoken expiringToken = new Vaulttoken();
        expiringToken.setValid(true);
        expiringToken.setAccessToken("expiring-token");
        expiringToken.setExpiresBy(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(4).toInstant(ZoneOffset.UTC));
        ReflectionTestUtils.setField(vaultConfig, "vaultToken", expiringToken);

        when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                .thenReturn(createMockTokenResponse("new-refreshed-token", 3600)) // New token
                .thenReturn(createMockSecretsResponse()); // Secrets response

        // Act: init() calls getSecrets() which in turn calls getToken()
        vaultConfig.init();

        // Assert
        verify(restTemplate, times(2)).exchange(requestEntityCaptor.capture(), eq(String.class));

        // Verify the second call was for secrets and used the *new* token
        RequestEntity<?> secretsRequest = requestEntityCaptor.getAllValues().get(1);
        assertEquals("Bearer new-refreshed-token", secretsRequest.getHeaders().getFirst("Authorization"));
    }

    @Test
    @DisplayName("getToken() - No Valid Token Can Be Obtained - Should Throw VaultTokenException")
    void getToken_whenNoValidToken_shouldThrowVaultTokenException() {
        // Arrange: Mock the token endpoint to return an invalid response which causes an invalid token object
        when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"invalid_json\": true}", HttpStatus.OK));

        // Act & Assert
        VaultTokenException exception = assertThrows(VaultTokenException.class, () -> vaultConfig.init());
//        assertEquals("Cannot proceed without a valid Vault token.", exception.getMessage());
        assertTrue(exception.getMessage().contains("Error refreshing Vault token"));
    }
}