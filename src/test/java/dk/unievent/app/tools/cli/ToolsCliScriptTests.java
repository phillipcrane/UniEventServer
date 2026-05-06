package dk.unievent.app.tools.cli;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolsCliScriptTests {

    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test
    void toolsDispatcherShouldReferenceExistingCommandScripts() throws IOException {
        String dispatcher = read("tools.ps1");
        Map<String, String> commandScripts = Map.of(
            "setup", "setup.ps1",
            "vault", "vault.ps1",
            "docker", "docker.ps1",
            "status", "status.ps1",
            "seed", "seed.ps1",
            "refresh", "refresh.ps1",
            "ingest", "ingest.ps1",
            "invite", "invite.ps1"
        );

        assertAll(commandScripts.entrySet().stream()
            .map(entry -> () -> {
                assertTrue(dispatcher.contains("\"" + entry.getKey() + "\""),
                    "Dispatcher should list command: " + entry.getKey());
                assertTrue(dispatcher.contains("\"" + entry.getValue() + "\""),
                    "Dispatcher should load script: " + entry.getValue());
                assertTrue(Files.exists(ROOT.resolve("cli").resolve(entry.getValue())),
                    "Script should exist: " + entry.getValue());
            }));
    }

    @Test
    void commandScriptsShouldExposeExpectedInvokeFunctions() throws IOException {
        Map<String, String> scriptFunctions = Map.of(
            "setup.ps1", "Invoke-Setup",
            "vault.ps1", "Invoke-VaultSetup",
            "docker.ps1", "Invoke-Docker",
            "status.ps1", "Invoke-Status",
            "seed.ps1", "Invoke-Seed",
            "refresh.ps1", "Invoke-Refresh",
            "ingest.ps1", "Invoke-Ingest",
            "invite.ps1", "Invoke-TestOrganizerKey"
        );

        assertAll(scriptFunctions.entrySet().stream()
            .map(entry -> () -> {
                String script = read("cli/" + entry.getKey());
                assertTrue(script.matches("(?s).*function\\s+" + entry.getValue() + "\\b.*"),
                    entry.getKey() + " should define " + entry.getValue());
            }));
    }

    @Test
    void serverBackedCommandScriptsShouldTargetDocumentedAdminEndpoints() throws IOException {
        String seed = read("cli/seed.ps1");
        String ingest = read("cli/ingest.ps1");
        String refresh = read("cli/refresh.ps1");
        String invite = read("cli/invite.ps1");

        assertAll(
            () -> assertTrue(seed.contains("-Method \"DELETE\" -Url \"$BaseUrl/admin/tools/seed\"")),
            () -> assertTrue(seed.contains("-Method \"POST\" -Url \"$BaseUrl/admin/tools/seed\"")),
            () -> assertTrue(ingest.contains("-Method \"GET\" -Url \"$BaseUrl/admin/tools/pages\"")),
            () -> assertTrue(ingest.contains("-Method \"POST\" -Url \"$BaseUrl/admin/tools/ingest/$encodedPageId\"")),
            () -> assertTrue(refresh.contains("-Method \"POST\" -Url \"$BaseUrl/admin/tools/refresh-tokens\"")),
            () -> assertTrue(refresh.contains("-Method \"POST\" -Url \"$BaseUrl/admin/tools/refresh-tokens/$encodedPage\"")),
            () -> assertTrue(invite.contains("-Method \"POST\" -Url \"$BaseUrl/api/auth/organizer-key/generate\""))
        );
    }

    @Test
    void helpTextShouldDocumentAllTopLevelCommands() throws IOException {
        String dispatcher = read("tools.ps1");

        assertAll(
            () -> assertTrue(dispatcher.contains("setup                  Check dependencies")),
            () -> assertTrue(dispatcher.contains("docker                 Start")),
            () -> assertTrue(dispatcher.contains("vault                  Initialize")),
            () -> assertTrue(dispatcher.contains("seed                   Clear")),
            () -> assertTrue(dispatcher.contains("refresh                Refresh")),
            () -> assertTrue(dispatcher.contains("ingest                 Manually ingest")),
            () -> assertTrue(dispatcher.contains("invite                 Send organizer invite"))
        );
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
