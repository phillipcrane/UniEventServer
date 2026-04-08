package dk.unievent.app.application.mapper;

import dk.unievent.app.application.dto.SecretDTO;
import dk.unievent.app.db.model.SecretEntity;
import org.springframework.stereotype.Component;

@Component
public class SecretMapper {

    public SecretDTO toDTO(SecretEntity entity) {
        if (entity == null) {
            return null;
        }

        return new SecretDTO(
            entity.getId(),
            entity.getName(),
            entity.getSecretType(),
            entity.getVaultPath(),
            entity.getVaultKey(),
            entity.getStatus(),
            entity.getLastSyncedAt(),
            entity.getExpiresAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public SecretEntity toEntity(SecretDTO dto) {
        if (dto == null) {
            return null;
        }

        return SecretEntity.builder()
            .id(dto.getId())
            .name(dto.getName())
            .secretType(dto.getSecretType())
            .vaultPath(dto.getVaultPath())
            .vaultKey(dto.getVaultKey())
            .status(dto.getStatus())
            .lastSyncedAt(dto.getLastSyncedAt())
            .expiresAt(dto.getExpiresAt())
            .build();
    }
}
