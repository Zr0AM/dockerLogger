package org.omnomnom.dockerLogger.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONObject;

public final class Converter {
    private static final Logger LOGGER = LoggerFactory.getLogger(Converter.class);

    public static String getStringFromJson(JSONObject json, String key) {
        return json.optString(key);
    }

    public static JSONObject getJsonObjectFromString(String input) {
        try {
            return new JSONObject(input);
        } catch (Exception e) {
            LOGGER.error("Could not convert JSON from String {}", input, e);
            return null;
        }
    }

}
