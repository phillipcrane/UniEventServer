package dk.unievent.app.dto;

import dk.unievent.app.application.dto.PageDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageDTOTests {

    @Test
    void gettersAndSettersShouldStorePageFields() {
        PageDTO dto = new PageDTO();

        dto.setId("page-1");
        dto.setName("UniEvent Page");
        dto.setUrl("https://facebook.com/unievent");
        dto.setActive(true);
        dto.setPictureId(5L);

        assertEquals("page-1", dto.getId());
        assertEquals("UniEvent Page", dto.getName());
        assertEquals("https://facebook.com/unievent", dto.getUrl());
        assertEquals(true, dto.getActive());
        assertEquals(5L, dto.getPictureId());
    }
}
