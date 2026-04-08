package dk.unievent.app.dto;

import dk.unievent.app.application.dto.LocationDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocationDTOTests {

    @Test
    void allArgsConstructorShouldPopulateAllFields() {
        LocationDTO dto = new LocationDTO(
            "Skjoldungsvej 100",
            "Lyngby",
            "2800",
            "Denmark",
            55.7842,
            12.4933
        );

        assertEquals("Skjoldungsvej 100", dto.getStreet());
        assertEquals("Lyngby", dto.getCity());
        assertEquals("2800", dto.getZip());
        assertEquals("Denmark", dto.getCountry());
        assertEquals(55.7842, dto.getLatitude());
        assertEquals(12.4933, dto.getLongitude());
    }
}
