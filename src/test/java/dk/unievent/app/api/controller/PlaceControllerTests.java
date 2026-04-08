package dk.unievent.app.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.unievent.app.application.dto.LocationDTO;
import dk.unievent.app.application.dto.PlaceDTO;
import dk.unievent.app.application.service.PlaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PlaceControllerTests {

    @Mock
    private PlaceService placeService;

    @InjectMocks
    private PlaceController placeController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(placeController).build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void getPlaceByIdShouldReturnNotFoundWhenMissing() throws Exception {
        when(placeService.getPlaceById("missing")).thenReturn(null);

        mockMvc.perform(get("/api/places/missing"))
            .andExpect(status().isNotFound());
    }

    @Test
    void createPlaceShouldReturnCreated() throws Exception {
        PlaceDTO created = samplePlace("place-1", "S-huset");
        when(placeService.createPlace(any(PlaceDTO.class))).thenReturn(created);

        mockMvc.perform(post("/api/places")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samplePlace(null, "S-huset"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("place-1"));
    }

    @Test
    void getPlacesByCityShouldReturnList() throws Exception {
        when(placeService.getPlacesByCity("Copenhagen")).thenReturn(List.of(samplePlace("place-1", "Venue")));

        mockMvc.perform(get("/api/places/city/Copenhagen"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("place-1"));
    }

    @Test
    void updatePlaceShouldReturnNotFoundWhenServiceReturnsNull() throws Exception {
        when(placeService.updatePlace(eq("place-404"), any(PlaceDTO.class))).thenReturn(null);

        mockMvc.perform(put("/api/places/place-404")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samplePlace("other-id", "Name"))))
            .andExpect(status().isNotFound());

        ArgumentCaptor<PlaceDTO> captor = ArgumentCaptor.forClass(PlaceDTO.class);
        verify(placeService).updatePlace(eq("place-404"), captor.capture());
        assertEquals("place-404", captor.getValue().getId());
    }

    @Test
    void deletePlaceShouldReturnNoContentWhenDeleted() throws Exception {
        when(placeService.deletePlace("place-1")).thenReturn(true);

        mockMvc.perform(delete("/api/places/place-1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deletePlaceShouldReturnNotFoundWhenMissing() throws Exception {
        when(placeService.deletePlace("missing")).thenReturn(false);

        mockMvc.perform(delete("/api/places/missing"))
            .andExpect(status().isNotFound());
    }

    private PlaceDTO samplePlace(String id, String name) {
        PlaceDTO dto = new PlaceDTO();
        dto.setId(id);
        dto.setName(name);

        LocationDTO location = new LocationDTO();
        location.setStreet("Skjoldungsvej 100");
        location.setCity("Lyngby");
        location.setZip("2800");
        location.setCountry("Denmark");
        location.setLatitude(55.7842);
        location.setLongitude(12.4933);

        dto.setLocation(location);
        return dto;
    }
}
