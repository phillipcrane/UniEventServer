package dk.unievent.app.model;

import dk.unievent.app.db.model.MediaEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MediaEntityTests {

    @Test
    void builderShouldPopulateAllFields() {
        Instant uploaded = Instant.now();
        MediaEntity entity = MediaEntity.builder()
            .id(1L)
            .filename("poster.jpg")
            .contentType("image/jpeg")
            .fileId("1,abc")
            .uploadedAt(uploaded)
            .build();

        assertEquals(1L, entity.getId());
        assertEquals("poster.jpg", entity.getFilename());
        assertEquals("image/jpeg", entity.getContentType());
        assertEquals("1,abc", entity.getFileId());
        assertEquals(uploaded, entity.getUploadedAt());
    }
}
