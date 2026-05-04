package dk.unievent.app.application.scheduler;

import dk.unievent.app.application.service.FacebookTokenRefreshService;
import dk.unievent.app.infrastructure.config.SchedulingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FacebookTokenRefresher {

    private final FacebookTokenRefreshService tokenRefreshService;

    public FacebookTokenRefresher(FacebookTokenRefreshService tokenRefreshService) {
        this.tokenRefreshService = tokenRefreshService;
    }

    @Scheduled(fixedDelay = SchedulingConstants.TOKEN_REFRESH_INTERVAL_MS, initialDelay = SchedulingConstants.TOKEN_REFRESH_INITIAL_DELAY_MS)
    public void refreshPageTokens() {
        try {
            tokenRefreshService.refreshAll();
        } catch (Exception e) {
            log.error("Unexpected error in Facebook token refresh scheduler", e);
        }
    }
}
