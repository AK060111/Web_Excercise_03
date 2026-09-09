import java.util.Map;
import jakarta.persistence.Persistence;
public class CheckMapping {
    public static void main(String[] args) {
        var factory = Persistence.createEntityManagerFactory("ServletCRUDMVC", Map.of(
            "hibernate.boot.allow_jdbc_metadata_access", "false",
            "hibernate.connection.provider_class", "org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl"));
        try { System.out.println("OFFLINE MAPPING OK: entities=" + factory.getMetamodel().getEntities().size()); }
        finally { factory.close(); }
    }
}
