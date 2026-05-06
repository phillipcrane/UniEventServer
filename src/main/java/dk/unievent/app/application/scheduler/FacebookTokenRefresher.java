package dk.unievent.app.application.scheduler;

import dk.unievent.app.application.service.TokenRefreshService;
import dk.unievent.app.infrastructure.constants.SchedulingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// thin scheduler wrapper around TokenRefreshService. The real logic lives there so both this
// scheduled job and the manual admin endpoint share the same code path.
@Slf4j
@Component
public class FacebookTokenRefresher {

    private final TokenRefreshService tokenRefreshService;

    public FacebookTokenRefresher(TokenRefreshService tokenRefreshService) {
        this.tokenRefreshService = tokenRefreshService;
    }

    @Scheduled(fixedDelay = SchedulingConstants.TOKEN_REFRESH_INTERVAL_MS, initialDelay = SchedulingConstants.TOKEN_REFRESH_INITIAL_DELAY_MS)
    public void refreshPageTokens() {
        try {
            tokenRefreshService.refreshAll();
        } catch (Exception e) {
            log.error("Unexpected error in Facebook token refresh scheduler", e); // log but don't rethrow, Spring reschedules the next run regardless
        }
    }
}
