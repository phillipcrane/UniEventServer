package dk.unievent.app.dto;

import dk.unievent.app.application.dto.MediaDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MediaDTOTests {

    @Test
    void builderShouldCreateDtoWithExpectedValues() {
        Instant now = Instant.now();

        MediaDTO dto = MediaDTO.builder()
            .id(7L)
            .filename("poster.png")
            .contentType("image/png")
            .fileId("1,abc")
            .uploadedAt(now)
            .build();

        assertEquals(7L, dto.getId());
        assertEquals("poster.png", dto.getFilename());
        assertEquals("image/png", dto.getContentType());
        assertEquals("1,abc", dto.getFileId());
        assertEquals(now, dto.getUploadedAt());
    }
}
