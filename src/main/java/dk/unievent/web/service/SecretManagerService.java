package dk.unievent.web.service;

import com.google.cloud.secretmanager.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SecretManagerService {

    private final SecretManagerServiceClient client;

    @Value("${gcp.project.id}")
    private String projectId;

    public SecretManagerService() throws IOException {
        this.client = SecretManagerServiceClient.create();
    }

    public void addPageToken(String pageId, String token, long expiresIn) throws Exception {
        String secretId = "facebook-token-" + pageId;
        String parent = ProjectName.format(projectId);

        // Create secret if it doesn't exist
        try {
            client.createSecret(parent, secretId, Secret.newBuilder().build());
        } catch (Exception e) {
            if (!e.getMessage().contains("Already exists")) {
                throw e;
            }
        }

        // Add version
        String secretVersionName = SecretVersionName.of(projectId, secretId, "latest").toString();
        client.addSecretVersion(secretVersionName, SecretPayload.newBuilder()
            .setData(ByteString.copyFromUtf8(token))
            .build());
    }

    public void updatePageToken(String pageId, String token, long expiresIn) throws Exception {
        addPageToken(pageId, token, expiresIn);
    }

    public String getPageToken(String pageId) throws Exception {
        String secretId = "facebook-token-" + pageId;
        String secretVersionName = SecretVersionName.of(projectId, secretId, "latest").toString();

        try {
            AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
            return response.getPayload().getData().toStringUtf8();
        } catch (Exception e) {
            if (e.getMessage().contains("NOT_FOUND")) {
                return null;
            }
            throw e;
        }
    }

    public boolean checkTokenExpiry(String pageId) {
        return false; // Simplified
    }
}