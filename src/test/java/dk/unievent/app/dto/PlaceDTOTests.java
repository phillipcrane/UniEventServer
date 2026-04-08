package dk.unievent.app.dto;

import dk.unievent.app.application.dto.LocationDTO;
import dk.unievent.app.application.dto.PlaceDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaceDTOTests {

    @Test
    void settersShouldStoreNestedLocation() {
        PlaceDTO dto = new PlaceDTO();
        LocationDTO location = new LocationDTO();
        location.setStreet("Skjoldungsvej 100");
        location.setCity("Lyngby");
        location.setZip("2800");
        location.setCountry("Denmark");
        location.setLatitude(55.7842);
        location.setLongitude(12.4933);

        dto.setId("place-1");
        dto.setName("S-huset");
        dto.setLocation(location);

        assertEquals("place-1", dto.getId());
        assertEquals("S-huset", dto.getName());
        assertEquals("Lyngby", dto.getLocation().getCity());
        assertEquals("2800", dto.getLocation().getZip());
    }
}
