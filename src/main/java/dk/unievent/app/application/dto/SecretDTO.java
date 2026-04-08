package dk.unievent.app.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SecretDTO {
    private Long id;
    private String name;
    private String secretType;
    private String vaultPath;
    private String vaultKey;
    private String status;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
