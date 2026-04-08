package dk.unievent.app.service;

import dk.unievent.app.application.dto.SecretDTO;
import dk.unievent.app.application.mapper.SecretMapper;
import dk.unievent.app.application.service.VaultService;
import dk.unievent.app.db.model.SecretEntity;
import dk.unievent.app.db.repository.SecretRepository;
import dk.unievent.app.infrastructure.client.VaultClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VaultServiceTests {

    @Mock
    private VaultClient vaultClient;

    @Mock
    private SecretRepository secretRepository;

    @Mock
    private SecretMapper secretMapper;

    @InjectMocks
    private VaultService vaultService;

    @Test
    void testReadVaultSecretDataDelegatesToClient() {
        Map<String, String> expected = Map.of("username", "admin", "password", "secret");
        when(vaultClient.readSecretData()).thenReturn(expected);

        Map<String, String> result = vaultService.readVaultSecretData();

        assertEquals(expected, result);
        verify(vaultClient, times(1)).readSecretData();
    }

    @Test
    void testReadVaultSecretValueDelegatesToClient() {
        when(vaultClient.readSecretValue("password")).thenReturn("value-123");

        String result = vaultService.readVaultSecretValue("password");

        assertEquals("value-123", result);
        verify(vaultClient, times(1)).readSecretValue("password");
    }

    @Test
    void testGetAllSecretsMapsRepositoryResult() {
        SecretEntity first = SecretEntity.builder().id(1L).name("a").secretType("db").vaultPath("p1").build();
        SecretEntity second = SecretEntity.builder().id(2L).name("b").secretType("api").vaultPath("p2").build();

        SecretDTO firstDto = new SecretDTO();
        firstDto.setId(1L);
        firstDto.setName("a");

        SecretDTO secondDto = new SecretDTO();
        secondDto.setId(2L);
        secondDto.setName("b");

        when(secretRepository.findAll()).thenReturn(List.of(first, second));
        when(secretMapper.toDTO(first)).thenReturn(firstDto);
        when(secretMapper.toDTO(second)).thenReturn(secondDto);

        List<SecretDTO> result = vaultService.getAllSecrets();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        verify(secretRepository, times(1)).findAll();
        verify(secretMapper, times(2)).toDTO(any(SecretEntity.class));
    }

    @Test
    void testGetSecretByIdFound() {
        SecretEntity entity = SecretEntity.builder().id(9L).name("redis").secretType("cache").vaultPath("vp").build();
        SecretDTO dto = new SecretDTO();
        dto.setId(9L);
        dto.setName("redis");

        when(secretRepository.findById(9L)).thenReturn(Optional.of(entity));
        when(secretMapper.toDTO(entity)).thenReturn(dto);

        Optional<SecretDTO> result = vaultService.getSecretById(9L);

        assertTrue(result.isPresent());
        assertEquals("redis", result.get().getName());
    }

    @Test
    void testGetSecretByIdNotFound() {
        when(secretRepository.findById(404L)).thenReturn(Optional.empty());

        Optional<SecretDTO> result = vaultService.getSecretById(404L);

        assertTrue(result.isEmpty());
        verify(secretMapper, never()).toDTO(any());
    }

    @Test
    void testGetSecretByNameFound() {
        SecretEntity entity = SecretEntity.builder().id(5L).name("jwt").secretType("api").vaultPath("v").build();
        SecretDTO dto = new SecretDTO();
        dto.setId(5L);
        dto.setName("jwt");

        when(secretRepository.findByName("jwt")).thenReturn(Optional.of(entity));
        when(secretMapper.toDTO(entity)).thenReturn(dto);

        Optional<SecretDTO> result = vaultService.getSecretByName("jwt");

        assertTrue(result.isPresent());
        assertEquals(5L, result.get().getId());
        assertEquals("jwt", result.get().getName());
    }

    @Test
    void testGetSecretByNameNotFound() {
        when(secretRepository.findByName("missing")).thenReturn(Optional.empty());

        Optional<SecretDTO> result = vaultService.getSecretByName("missing");

        assertTrue(result.isEmpty());
        verify(secretMapper, never()).toDTO(any());
    }

    @Test
    void testSaveSecretSetsLastSyncedAtAndReturnsMappedDto() {
        SecretDTO input = new SecretDTO();
        input.setName("new-secret");
        input.setSecretType("database");
        input.setVaultPath("secret/data/new");

        SecretEntity mapped = SecretEntity.builder()
            .name("new-secret")
            .secretType("database")
            .vaultPath("secret/data/new")
            .build();

        SecretEntity saved = SecretEntity.builder()
            .id(77L)
            .name("new-secret")
            .secretType("database")
            .vaultPath("secret/data/new")
            .lastSyncedAt(LocalDateTime.now())
            .build();

        SecretDTO savedDto = new SecretDTO();
        savedDto.setId(77L);
        savedDto.setName("new-secret");

        when(secretMapper.toEntity(input)).thenReturn(mapped);
        when(secretRepository.save(any(SecretEntity.class))).thenReturn(saved);
        when(secretMapper.toDTO(saved)).thenReturn(savedDto);

        SecretDTO result = vaultService.saveSecret(input);

        assertNotNull(result);
        assertEquals(77L, result.getId());

        ArgumentCaptor<SecretEntity> captor = ArgumentCaptor.forClass(SecretEntity.class);
        verify(secretRepository, times(1)).save(captor.capture());
        assertNotNull(captor.getValue().getLastSyncedAt());
    }

    @Test
    void testDeleteSecretWhenExists() {
        when(secretRepository.existsById(10L)).thenReturn(true);

        boolean result = vaultService.deleteSecret(10L);

        assertTrue(result);
        verify(secretRepository, times(1)).deleteById(10L);
    }

    @Test
    void testDeleteSecretWhenMissing() {
        when(secretRepository.existsById(999L)).thenReturn(false);

        boolean result = vaultService.deleteSecret(999L);

        assertFalse(result);
        verify(secretRepository, never()).deleteById(anyLong());
    }
}
