package dk.unievent.app.tools.cli;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeploymentConfigContractTests {

    @Test
    void backendDockerfileShouldBuildLayeredJarAndRunAsUnprivilegedUser() throws IOException {
        String dockerfile = file("Dockerfile");

        assertAll(
            () -> assertTrue(dockerfile.contains("FROM eclipse-temurin:25.0.3_9-jdk-alpine-3.23 AS builder")),
            () -> assertTrue(dockerfile.contains("RUN ./mvnw package -DskipTests -B")),
            () -> assertTrue(dockerfile.contains("java -Djarmode=tools -jar target/web-*.jar extract")),
            () -> assertTrue(dockerfile.contains("FROM eclipse-temurin:25.0.3_9-jre-alpine")),
            () -> assertTrue(dockerfile.contains("adduser -S app -G app")),
            () -> assertTrue(dockerfile.contains("USER app")),
            () -> assertTrue(dockerfile.contains("EXPOSE 8080")),
            () -> assertTrue(dockerfile.contains("JarLauncher"))
        );
    }

    @Test
    void frontendDockerfileShouldBuildWithNodeAndServeStaticBundleWithNginx() throws IOException {
        String dockerfile = file("web/Dockerfile");

        assertAll(
            () -> assertTrue(dockerfile.contains("FROM node:22.22-alpine3.22 AS builder")),
            () -> assertTrue(dockerfile.contains("RUN npm ci")),
            () -> assertTrue(dockerfile.contains("ARG VITE_BACKEND_URL")),
            () -> assertTrue(dockerfile.contains("ARG VITE_FACEBOOK_APP_ID")),
            () -> assertTrue(dockerfile.contains("RUN npm run build")),
            () -> assertTrue(dockerfile.contains("FROM nginx:1.30.0-alpine")),
            () -> assertTrue(dockerfile.contains("COPY --from=builder /app/dist /usr/share/nginx/html")),
            () -> assertTrue(dockerfile.contains("EXPOSE 3000"))
        );
    }

    @Test
    void nginxConfigsShouldProxyBackendRoutesAndFallbackToFrontend() throws IOException {
        String dev = file("deploy/nginx-dev.conf");
        String prod = file("deploy/nginx-https.conf");

        assertAll(
            () -> assertRouteToBackend(dev, "/api/"),
            () -> assertRouteToBackend(dev, "/media/"),
            () -> assertRouteToBackend(dev, "/actuator/"),
            () -> assertRouteToBackend(prod, "/api/"),
            () -> assertRouteToBackend(prod, "/media/"),
            () -> assertRouteToBackend(prod, "/actuator/"),
            () -> assertTrue(dev.contains("proxy_pass http://frontend:3000")),
            () -> assertTrue(prod.contains("proxy_pass http://frontend:3000"))
        );
    }

    @Test
    void productionNginxShouldKeepSecurityHeadersAndAuthRateLimit() throws IOException {
        String prod = file("deploy/nginx-https.conf");

        assertAll(
            () -> assertTrue(prod.contains("server_tokens off")),
            () -> assertTrue(prod.contains("limit_req_zone $binary_remote_addr zone=auth:10m rate=10r/m")),
            () -> assertTrue(prod.contains("location /api/auth/")),
            () -> assertTrue(prod.contains("limit_req zone=auth burst=5 nodelay")),
            () -> assertTrue(prod.contains("Strict-Transport-Security")),
            () -> assertTrue(prod.contains("X-Content-Type-Options")),
            () -> assertTrue(prod.contains("X-Frame-Options")),
            () -> assertTrue(prod.contains("Referrer-Policy")),
            () -> assertTrue(prod.contains("Permissions-Policy")),
            () -> assertFalse(prod.contains("location /admin/"))
        );
    }

    @Test
    void vaultPolicyShouldGrantReadOnlyBaseSecretAccessAndScopedFacebookTokenWrites() throws IOException {
        String policy = file("vault/config/policies/unievent-app.hcl");

        assertAll(
            () -> assertTrue(policy.contains("path \"secret/data/unievent\"")),
            () -> assertTrue(policy.contains("capabilities = [\"read\"]")),
            () -> assertTrue(policy.contains("path \"secret/data/unievent/facebook/page_*\"")),
            () -> assertTrue(policy.contains("capabilities = [\"create\", \"read\", \"update\"]")),
            () -> assertFalse(policy.contains("\"delete\"")),
            () -> assertFalse(policy.contains("secret/data/*"))
        );
    }

    @Test
    void ciWorkflowShouldGateBackendFrontendAndDockerBuilds() throws IOException {
        String ci = file(".github/workflows/ci.yml");

        assertAll(
            () -> assertTrue(ci.contains("branches: [main]")),
            () -> assertTrue(ci.contains("pull_request:")),
            () -> assertTrue(ci.contains("java-version: '25'")),
            () -> assertTrue(ci.contains("run: ./mvnw test -B")),
            () -> assertTrue(ci.contains("node-version: '22'")),
            () -> assertTrue(ci.contains("run: cd web && npm test")),
            () -> assertTrue(ci.contains("docker build -t unievent-app:ci .")),
            () -> assertTrue(ci.contains("docker build ./web")),
            () -> assertTrue(ci.contains("-t unievent-frontend:ci"))
        );
    }

    @Test
    void deployWorkflowShouldTestBeforeProductionDeploy() throws IOException {
        String deploy = file(".github/workflows/deploy.yml");

        assertAll(
            () -> assertTrue(deploy.contains("branches: [live]")),
            () -> assertTrue(deploy.contains("build-and-test:")),
            () -> assertTrue(deploy.contains("run: ./mvnw test -B")),
            () -> assertTrue(deploy.contains("run: npm ci && npm run build")),
            () -> assertTrue(deploy.contains("needs: build-and-test")),
            () -> assertTrue(deploy.contains("fingerprint: ${{ secrets.LIVE_DEPLOY_FINGERPRINT }}")),
            () -> assertTrue(deploy.contains("certbot certonly --webroot")),
            () -> assertTrue(deploy.contains("docker compose up --build -d"))
        );
    }

    private static void assertRouteToBackend(String config, String route) {
        assertTrue(config.contains("location " + route), "Missing route " + route);
        assertTrue(config.contains("proxy_pass http://app:8080"), "Missing backend proxy for " + route);
    }

    private static String file(String path) throws IOException {
        return Files.readString(Path.of(path)).replace("\r\n", "\n");
    }
}
