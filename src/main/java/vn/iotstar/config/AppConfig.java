package vn.iotstar.config;

import java.io.IOException;
import java.util.Properties;

/** System properties override environment variables, then application.properties. */
public final class AppConfig {
    private static final Properties VALUES = new Properties();
    static {
        try (var input = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (input != null) VALUES.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read application.properties", e);
        }
    }
    private AppConfig() { }
    public static String get(String key, String environment) {
        String value = System.getProperty(key);
        if (value == null) value = System.getenv(environment);
        return value == null ? VALUES.getProperty(key) : value;
    }
}
