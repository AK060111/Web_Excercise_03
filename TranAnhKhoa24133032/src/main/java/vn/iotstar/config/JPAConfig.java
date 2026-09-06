package vn.iotstar.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.Map;

public class JPAConfig {
    private static EntityManagerFactory factory;

    public static synchronized EntityManager getEntityManager() {
        if (factory == null || !factory.isOpen()) {
            factory = Persistence.createEntityManagerFactory("ServletCRUDMVC", Map.of(
                "jakarta.persistence.jdbc.url", AppConfig.get("db.url", "DB_URL"),
                "jakarta.persistence.jdbc.user", AppConfig.get("db.user", "DB_USER"),
                "jakarta.persistence.jdbc.password", AppConfig.get("db.password", "DB_PASSWORD")));
        }
        return factory.createEntityManager();
    }

    public static synchronized void close() {
        if (factory != null && factory.isOpen()) factory.close();
        factory = null;
    }
}
