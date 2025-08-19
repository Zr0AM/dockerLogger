package org.omnomnom.dockerlogger.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omnomnom.dockerlogger.db.Logentity;
import org.omnomnom.dockerlogger.service.LogService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link LogController}.
 *
 * These tests use Mockito to mock the {@link LogService} dependency,
 * allowing for focused testing of the controller's logic without
 * involving the actual service implementation or database.
 */
@Profile("!DEV")
@ExtendWith(MockitoExtension.class)
class LogControllerTest {

    // Mock the service layer dependency
    @Mock
    private LogService logService;

    // Inject the mock into the controller instance
    @InjectMocks
    private LogController logController;

    private Logentity testLog;

    @BeforeEach
    void setUp() {
        // Initialize a common test object to be used in multiple tests
        testLog = new Logentity();
    }

    @Test
    @DisplayName("addLog() - Valid Log - Should Return 204 No Content")
    void addLog_validLog_shouldReturnNoContent() {
        // Arrange
        // Configure the mock service to successfully process the log insertion.
        // For a void method, doNothing() is the default behavior, but we state it for clarity.
        doNothing().when(logService).insertLog(any(Logentity.class));

        // Act
        ResponseEntity<String> response = logController.addLog(testLog);

        // Assert
        // Verify that the service's insertLog method was called exactly once with our test log.
        verify(logService, times(1)).insertLog(testLog);

        // Verify that the controller returns the correct HTTP status for a successful, non-returning operation.
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody(), "Response body should be null for a 204 No Content status.");
    }

    @Test
    @DisplayName("addLog() - Service Throws Exception - Should Return 500 Internal Server Error")
    void addLog_serviceThrowsException_shouldReturnInternalServerError() {
        // Arrange
        String errorMessage = "Database connection failed";
        // Configure the mock service to throw an exception, simulating a service-layer error.
        doThrow(new RuntimeException(errorMessage)).when(logService).insertLog(any(Logentity.class));

        // Act
        ResponseEntity<String> response = logController.addLog(testLog);

        // Assert
        // Verify that the service method was still called.
        verify(logService, times(1)).insertLog(testLog);

        // Verify that the controller caught the exception and returned a 500 status.
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        // Verify that the response body contains a user-friendly error message including the exception's message.
        String expectedBody = "Error adding log: " + errorMessage;
        assertEquals(expectedBody, response.getBody());
    }

    @Test
    @DisplayName("addLog() - Null Input - Should Be Handled by Catch Block and Return 500")
    void addLog_nullInput_shouldReturnInternalServerError() {
        // Arrange: Simulate the service throwing an exception for null input
        doThrow(new IllegalArgumentException("Log entity cannot be null")).when(logService).insertLog(null);

        // Act
        ResponseEntity<String> response = logController.addLog(null);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error adding log: Log entity cannot be null", response.getBody());
        verify(logService, times(1)).insertLog(null);
    }
}