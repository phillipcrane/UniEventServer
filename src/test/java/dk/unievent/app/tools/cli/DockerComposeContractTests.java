package dk.unievent.app.tools.cli;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerComposeContractTests {

    private static final Path COMPOSE_FILE = Path.of("docker-compose.yml");

    @Test
    void composeFileShouldDefineExpectedRuntimeServices() throws IOException {
        Map<String, Object> services = services();

        assertAll(
            () -> assertTrue(services.containsKey("mysql")),
            () -> assertTrue(services.containsKey("vault")),
            () -> assertTrue(services.containsKey("weed-master")),
            () -> assertTrue(services.containsKey("weed-volume")),
            () -> assertTrue(services.containsKey("app")),
            () -> assertTrue(services.containsKey("frontend")),
            () -> assertTrue(services.containsKey("nginx"))
        );
    }

    @Test
    void statefulServicesShouldHaveHealthchecksAndPersistentVolumes() throws IOException {
        Map<String, Object> services = services();

        assertAll(
            () -> assertServiceHasHealthcheck(services, "mysql"),
            () -> assertServiceHasHealthcheck(services, "vault"),
            () -> assertServiceHasHealthcheck(services, "weed-master"),
            () -> assertServiceHasHealthcheck(services, "weed-volume"),
            () -> assertServiceMountsVolume(services, "mysql", "mysql-data:/var/lib/mysql"),
            () -> assertServiceMountsVolume(services, "vault", "vault-data:/vault/file"),
            () -> assertServiceMountsVolume(services, "weed-volume", "seaweedfs-data:/data")
        );
    }

    @Test
    void appShouldWaitForHealthyStatefulDependencies() throws IOException {
        Map<String, Object> app = service("app");
        @SuppressWarnings("unchecked")
        Map<String, Map<String, String>> dependsOn = (Map<String, Map<String, String>>) app.get("depends_on");

        assertNotNull(dependsOn);
        assertAll(
            () -> assertEquals("service_healthy", dependsOn.get("mysql").get("condition")),
            () -> assertEquals("service_healthy", dependsOn.get("vault").get("condition")),
            () -> assertEquals("service_healthy", dependsOn.get("weed-master").get("condition")),
            () -> assertEquals("service_healthy", dependsOn.get("weed-volume").get("condition"))
        );
    }

    @Test
    void localDevelopmentPortsShouldBindToLoopbackOnly() throws IOException {
        Map<String, Object> services = services();

        assertAll(
            () -> assertServicePortStartsWith(services, "mysql", "127.0.0.1:3307:3306"),
            () -> assertServicePortStartsWith(services, "vault", "127.0.0.1:8200:8200"),
            () -> assertServicePortStartsWith(services, "weed-master", "127.0.0.1:9333:9333"),
            () -> assertServicePortStartsWith(services, "app", "127.0.0.1:8080:8080"),
            () -> assertServicePortStartsWith(services, "frontend", "127.0.0.1:3000:3000")
        );
    }

    private static void assertServiceHasHealthcheck(Map<String, Object> services, String serviceName) {
        Map<String, Object> service = service(services, serviceName);
        assertNotNull(service.get("healthcheck"), serviceName + " should define a healthcheck");
    }

    private static void assertServiceMountsVolume(Map<String, Object> services, String serviceName, String volumePrefix) {
        Map<String, Object> service = service(services, serviceName);
        List<String> volumes = strings(service.get("volumes"));

        assertTrue(volumes.stream().anyMatch(volume -> volume.startsWith(volumePrefix)),
            serviceName + " should mount " + volumePrefix);
    }

    private static void assertServicePortStartsWith(Map<String, Object> services, String serviceName, String port) {
        Map<String, Object> service = service(services, serviceName);
        List<String> ports = strings(service.get("ports"));

        assertTrue(ports.contains(port), serviceName + " should publish " + port);
    }

    private static Map<String, Object> service(String serviceName) throws IOException {
        return service(services(), serviceName);
    }

    private static Map<String, Object> service(Map<String, Object> services, String serviceName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> service = (Map<String, Object>) services.get(serviceName);
        assertNotNull(service, serviceName + " service should exist");
        return service;
    }

    private static Map<String, Object> services() throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) yaml();
        @SuppressWarnings("unchecked")
        Map<String, Object> services = (Map<String, Object>) root.get("services");

        assertNotNull(services);
        assertFalse(services.isEmpty());
        return services;
    }

    private static Object yaml() throws IOException {
        try (Reader reader = Files.newBufferedReader(COMPOSE_FILE)) {
            return new Yaml().load(reader);
        }
    }

    private static List<String> strings(Object value) {
        assertTrue(value instanceof List<?>);
        return ((List<?>) value).stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .toList();
    }
}
