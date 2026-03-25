package dk.unievent.web.service;

import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

@Service
public class StorageService {

    private final Bucket bucket;

    public StorageService(Storage storage, @Value("${gcp.storage.bucket:unievent-images}") String bucketName) {
        this.bucket = storage.get(bucketName);
    }

    public String addImage(String filePath, byte[] data, String contentType) throws IOException {
        Blob blob = bucket.create(filePath, data, contentType);
        blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));
        return blob.getMediaLink();
    }

    @SuppressWarnings("deprecation")
    public String addImageFromUrl(String filePath, String sourceUrl) throws IOException {
        URL url = new URL(sourceUrl);
        // ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
        // Simplified: in real implementation, read the content type and data
        // For now, assume JPEG
        byte[] data = new byte[0]; // Read from channel
        return addImage(filePath + ".jpg", data, "image/jpeg");
    }

    public String getImage(String filePath) {
        Blob blob = bucket.get(filePath);
        if (blob == null || !blob.exists()) {
            return null;
        }
        return blob.getMediaLink();
    }

    public void removeImage(String filePath) {
        bucket.get(filePath).delete();
    }
}