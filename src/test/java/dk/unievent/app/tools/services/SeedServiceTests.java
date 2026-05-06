package dk.unievent.app.tools.services;

import dk.unievent.app.application.service.MediaService;
import dk.unievent.app.db.model.EventEntity;
import dk.unievent.app.db.model.MediaEntity;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.db.model.PlaceEntity;
import dk.unievent.app.db.repository.EventRepository;
import dk.unievent.app.db.repository.MediaRepository;
import dk.unievent.app.db.repository.PageRepository;
import dk.unievent.app.db.repository.PlaceRepository;
import dk.unievent.app.tools.models.SeedResponse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeedServiceTests {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PageRepository pageRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MediaService mediaService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private SeedService seedService;

    private void stubEmptyRepositories() {
        when(eventRepository.findAll()).thenReturn(List.of());
        when(pageRepository.findAll()).thenReturn(List.of());
        when(placeRepository.findAll()).thenReturn(List.of());
        when(mediaRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void seedDataShouldReturnSuccessWhenSeaweedFsAvailable() throws Exception {
        stubEmptyRepositories();
        when(mediaService.downloadAndStoreImage(any(), any())).thenReturn("1,abc");
        when(mediaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(placeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SeedResponse result = seedService.seedData();

        assertTrue(result.isSuccess());
        assertEquals(2, result.getPageCount());
        assertEquals(10, result.getEventCount());
        assertEquals(2, result.getPlaceCount());
        verify(entityManager, atLeast(1)).persist(any());
    }

    @Test
    void seedDataShouldSucceedWithoutMediaWhenSeaweedFsUnavailable() throws Exception {
        stubEmptyRepositories();
        when(mediaService.downloadAndStoreImage(any(), any())).thenThrow(new RuntimeException("SeaweedFS down"));
        when(placeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SeedResponse result = seedService.seedData();

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("no cover images"));
        assertEquals(2, result.getPageCount());
        assertEquals(10, result.getEventCount());
        verify(mediaRepository, never()).save(any());
    }

    @Test
    void seedDataShouldReturnFailureOnUnexpectedException() {
        stubEmptyRepositories();
        when(placeRepository.save(any())).thenThrow(new RuntimeException("DB exploded"));

        SeedResponse result = seedService.seedData();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Error during seed"));
        assertEquals(0, result.getPageCount());
    }

    @Test
    void seedDataShouldReturnFailureWhenInitialCleanupFails() throws Exception {
        when(eventRepository.findAll()).thenThrow(new RuntimeException("cleanup read failed"));

        SeedResponse result = seedService.seedData();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Error during seed"));
        verify(mediaService, never()).downloadAndStoreImage(any(), any());
    }

    @Test
    void clearSeedDataShouldFilterBySeedPrefix() {
        EventEntity seedEvent = EventEntity.builder().id("SEED_EVENT_001").title("Seeded").build();
        EventEntity realEvent = EventEntity.builder().id("real-event-id").title("Real").build();
        PageEntity seedPage = PageEntity.builder().id("SEED_TECH_EVENTS").name("Tech").build();
        PlaceEntity seedPlace = PlaceEntity.builder().id("place-uuid").name("SEED_Copenhagen").build();

        when(eventRepository.findAll()).thenReturn(List.of(seedEvent, realEvent));
        when(pageRepository.findAll()).thenReturn(List.of(seedPage));
        when(placeRepository.findAll()).thenReturn(List.of(seedPlace));
        when(mediaRepository.findAll()).thenReturn(List.of());

        SeedResponse result = seedService.clearSeedData();

        assertTrue(result.isSuccess());
        verify(eventRepository).deleteAll(List.of(seedEvent));
        verify(pageRepository).deleteAll(List.of(seedPage));
        verify(placeRepository).deleteAll(List.of(seedPlace));
    }

    @Test
    void clearSeedDataShouldDeleteUniqueSeedFilesAndContinueWhenStorageDeleteFails() throws Exception {
        EventEntity seedEvent = EventEntity.builder().id("SEED_EVENT_001").title("Seeded").build();
        PageEntity seedPage = PageEntity.builder().id("SEED_PAGE").name("Seeded page").build();
        PlaceEntity seedPlace = PlaceEntity.builder().id("place-uuid").name("SEED_Place").build();
        MediaEntity seedMedia1 = MediaEntity.builder().filename("SEED_one.jpg").fileId("1,shared").build();
        MediaEntity seedMedia2 = MediaEntity.builder().filename("SEED_two.jpg").fileId("1,shared").build();
        MediaEntity seedMedia3 = MediaEntity.builder().filename("SEED_three.jpg").fileId("1,other").build();
        MediaEntity seedMediaWithoutFile = MediaEntity.builder().filename("SEED_missing.jpg").fileId(null).build();
        MediaEntity realMedia = MediaEntity.builder().filename("real.jpg").fileId("1,real").build();

        when(eventRepository.findAll()).thenReturn(List.of(seedEvent));
        when(pageRepository.findAll()).thenReturn(List.of(seedPage));
        when(placeRepository.findAll()).thenReturn(List.of(seedPlace));
        when(mediaRepository.findAll()).thenReturn(List.of(seedMedia1, seedMedia2, seedMedia3, seedMediaWithoutFile, realMedia));
        doThrow(new IOException("already gone")).when(mediaService).delete("1,shared");

        SeedResponse result = seedService.clearSeedData();

        assertTrue(result.isSuccess());
        verify(mediaService, times(1)).delete("1,shared");
        verify(mediaService, times(1)).delete("1,other");
        verify(mediaService, never()).delete("1,real");
        verify(mediaService, never()).delete(null);
        verify(mediaRepository).deleteAll(List.of(seedMedia1, seedMedia2, seedMedia3, seedMediaWithoutFile));
    }

    @Test
    void clearSeedDataShouldReturnFailureOnException() {
        when(eventRepository.findAll()).thenThrow(new RuntimeException("DB error"));

        SeedResponse result = seedService.clearSeedData();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Error during cleanup"));
    }
}
