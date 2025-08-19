package org.omnomnom.dockerlogger.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omnomnom.dockerlogger.db.Logentity; // Make sure this import is correct
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Profile("!test")
@ExtendWith(MockitoExtension.class) // Integrates Mockito with JUnit 5
class LogServiceTest {

    @Mock // Creates a mock instance of JdbcTemplate
    private JdbcTemplate dbJdbcTemplate;

    @InjectMocks // Creates an instance of LogService and injects the mocks (@Mock) into it
    private LogService logService;

    @Captor // Captures arguments passed to mocked methods
    private ArgumentCaptor<Object[]> argsCaptor;

    @Captor
    private ArgumentCaptor<String> sqlCaptor;

    private Logentity testLog;

    @BeforeEach
    void setUp() {
        // Initialize a sample Logentity for testing
        testLog = new Logentity();
        testLog.setLogType("INFO");
        testLog.setLogApp("TestApp");
        testLog.setLogAppComp("TestComponent");
        testLog.setLogSrcIp("127.0.0.1");
        testLog.setLogSrcUser("testuser");
        testLog.setLogMsg("This is a test log message.");
        testLog.setLogError(true);
        testLog.setLogErrorStack("Test Stack Trace");
    }

    @Test
    void insertLog_shouldCallJdbcTemplateUpdateWithCorrectParameters() {
        // Arrange
        String expectedSql = "{call dbo.InsertLog(?, ?, ?, ?, ?, ?, ?, ?, ?)}";
        // We don't need to mock the return value of update() since it's void in this context
        // and we're just verifying the call.

        // Act
        logService.insertLog(testLog);

        // Assert
        // Verify that the update method was called exactly once
        verify(dbJdbcTemplate, times(1)).update(sqlCaptor.capture(), argsCaptor.capture());

        // Verify the SQL statement
        assertEquals(expectedSql, sqlCaptor.getValue());

        // Verify the arguments passed to the update method
        Object[] capturedArgs = argsCaptor.getValue();
        assertNotNull(capturedArgs);
        assertEquals(9, capturedArgs.length); // Expecting 9 arguments

        // Check the timestamp (allow for slight difference as it's LocalDateTime.now())
        assertInstanceOf(LocalDateTime.class, capturedArgs[0], "First argument should be LocalDateTime");
        // You could add a check here that the captured time is very close to LocalDateTime.now()
        // but checking the type is often sufficient for this kind of test.

        // Check the rest of the arguments from the Logentity object
        assertEquals(testLog.getLogType(), capturedArgs[1]);
        assertEquals(testLog.getLogApp(), capturedArgs[2]);
        assertEquals(testLog.getLogAppComp(), capturedArgs[3]);
        assertEquals(testLog.getLogSrcIp(), capturedArgs[4]);
        assertEquals(testLog.getLogSrcUser(), capturedArgs[5]);
        assertEquals(testLog.getLogMsg(), capturedArgs[6]);
        assertEquals(testLog.getLogError(), capturedArgs[7]);
        assertEquals(testLog.getLogErrorStack(), capturedArgs[8]);
    }

    @Test
    void insertLog_shouldHandleNullValuesInLogEntityGracefully() {
        // Arrange
        Logentity logWithNulls = new Logentity();
        // Set some values, leave others null
        logWithNulls.setLogType("WARN");
        logWithNulls.setLogApp("AppWithNulls");
        // logAppComp is null
        logWithNulls.setLogSrcIp("192.168.1.1");
        // logSrcUser is null
        logWithNulls.setLogMsg("Message with some null fields");
        // logError is null
        // logErrorStack is null

        String expectedSql = "{call dbo.InsertLog(?, ?, ?, ?, ?, ?, ?, ?, ?)}";

        // Act
        logService.insertLog(logWithNulls);

        // Assert
        verify(dbJdbcTemplate).update(sqlCaptor.capture(), argsCaptor.capture());

        assertEquals(expectedSql, sqlCaptor.getValue());
        Object[] capturedArgs = argsCaptor.getValue();
        assertEquals(9, capturedArgs.length);

        assertInstanceOf(LocalDateTime.class, capturedArgs[0]);
        assertEquals(logWithNulls.getLogType(), capturedArgs[1]);
        assertEquals(logWithNulls.getLogApp(), capturedArgs[2]);
        assertNull(capturedArgs[3]); // Check for null logAppComp
        assertEquals(logWithNulls.getLogSrcIp(), capturedArgs[4]);
        assertNull(capturedArgs[5]); // Check for null logSrcUser
        assertEquals(logWithNulls.getLogMsg(), capturedArgs[6]);
        assertNull(capturedArgs[7]); // Check for null logError
        assertNull(capturedArgs[8]); // Check for null logErrorStack
    }
}