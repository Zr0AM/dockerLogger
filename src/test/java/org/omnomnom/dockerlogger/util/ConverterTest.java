package org.omnomnom.dockerlogger.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the {@link Converter} utility class.
 * This class contains only static methods, so no mocking or instance setup is required.
 */
class ConverterTest {

    //================================================================================
    // Tests for getJsonObjectFromString(String)
    //================================================================================

    @Test
    @DisplayName("getJsonObjectFromString() - Valid JSON String - Should Return JSONObject")
    void getJsonObjectFromString_validJsonString_shouldReturnJsonObject() throws JSONException {
        // Arrange
        String validJson = "{\"name\":\"test-app\",\"version\":1.0,\"active\":true}";

        // Act
        JSONObject result = Converter.getJsonObjectFromString(validJson);

        // Assert
        assertNotNull(result, "JSONObject should not be null for valid input.");
        assertEquals("test-app", result.getString("name"));
        assertEquals(1.0, result.getDouble("version"));
        assertTrue(result.getBoolean("active"));
    }

    @Test
    @DisplayName("getJsonObjectFromString() - Invalid JSON String - Should Return Null")
    void getJsonObjectFromString_invalidJsonString_shouldReturnNull() {
        // Arrange
        String invalidJson = "{\"name\":\"test-app\",}"; // Malformed JSON with a trailing comma

        // Act
        JSONObject result = Converter.getJsonObjectFromString(invalidJson);

        // Assert
        assertNull(result, "JSONObject should be null for an invalid JSON string.");
    }

    @Test
    @DisplayName("getJsonObjectFromString() - Null Input - Should Return Null")
    void getJsonObjectFromString_nullInput_shouldReturnNull() {
        // Arrange
        String nullInput = null;

        // Act
        JSONObject result = Converter.getJsonObjectFromString(nullInput);

        // Assert
        assertNull(result, "JSONObject should be null for a null input string.");
    }

    @Test
    @DisplayName("getJsonObjectFromString() - Empty String Input - Should Return Null")
    void getJsonObjectFromString_emptyString_shouldReturnNull() {
        // Arrange
        String emptyString = "";

        // Act
        JSONObject result = Converter.getJsonObjectFromString(emptyString);

        // Assert
        assertNull(result, "JSONObject should be null for an empty string input.");
    }

    @Test
    @DisplayName("getJsonObjectFromString() - Whitespace String Input - Should Return Null")
    void getJsonObjectFromString_whitespaceString_shouldReturnNull() {
        // Arrange
        String whitespaceString = "   \t\n  ";

        // Act
        JSONObject result = Converter.getJsonObjectFromString(whitespaceString);

        // Assert
        assertNull(result, "JSONObject should be null for a whitespace-only string input.");
    }

    //================================================================================
    // Tests for getStringFromJson(JSONObject, String)
    //================================================================================

    @Test
    @DisplayName("getStringFromJson() - Valid Key - Should Return Correct Value")
    void getStringFromJson_validKey_shouldReturnValue() throws JSONException {
        // Arrange
        JSONObject json = new JSONObject("{\"key1\":\"value1\",\"key2\":\"value2\"}");
        String key = "key1";

        // Act
        String result = Converter.getStringFromJson(json, key);

        // Assert
        assertEquals("value1", result);
    }

    @Test
    @DisplayName("getStringFromJson() - Non-Existent Key - Should Return Empty String")
    void getStringFromJson_nonExistentKey_shouldReturnEmptyString() throws JSONException {
        // Arrange
        JSONObject json = new JSONObject("{\"key1\":\"value1\"}");
        String key = "nonexistent_key";

        // Act
        String result = Converter.getStringFromJson(json, key);

        // Assert
        assertEquals("", result, "Should return an empty string for a non-existent key.");
    }

    @Test
    @DisplayName("getStringFromJson() - Null JSON Input - Should Throw NullPointerException")
    void getStringFromJson_nullJson_shouldThrowNullPointerException() {
        // Arrange
        JSONObject json = null;
        String key = "anyKey";

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            Converter.getStringFromJson(json, key);
        }, "Should throw NullPointerException when the JSONObject is null.");
    }

    @Test
    @DisplayName("Constructor - Is Private - Should Not Be Instantiable")
    void constructor_isPrivate_shouldNotBeInstantiable() throws NoSuchMethodException {
        // Arrange
        Constructor<Converter> constructor = Converter.class.getDeclaredConstructor();

        // Assert that the constructor is private
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "The constructor should be private.");

    }
}