package dk.unievent.app.application.scheduler;

import dk.unievent.app.WebApplication;
import dk.unievent.app.application.service.RefreshTokenService;
import dk.unievent.app.infrastructure.constants.SchedulingConstants;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerConfigurationTests {

    @Test
    void applicationShouldEnableScheduling() {
        assertTrue(WebApplication.class.isAnnotationPresent(EnableScheduling.class));
    }

    @Test
    void facebookIngestionShouldUseConfiguredSchedule() throws Exception {
        Scheduled schedule = scheduled(FacebookIngestionScheduler.class, "ingestFacebookEvents");

        assertEquals(SchedulingConstants.INGESTION_INTERVAL_MS, schedule.fixedDelay());
        assertEquals(SchedulingConstants.INGESTION_INITIAL_DELAY_MS, schedule.initialDelay());
    }

    @Test
    void facebookTokenRefreshShouldUseConfiguredSchedule() throws Exception {
        Scheduled schedule = scheduled(FacebookTokenRefresher.class, "refreshPageTokens");

        assertEquals(SchedulingConstants.TOKEN_REFRESH_INTERVAL_MS, schedule.fixedDelay());
        assertEquals(SchedulingConstants.TOKEN_REFRESH_INITIAL_DELAY_MS, schedule.initialDelay());
    }

    @Test
    void refreshTokenCleanupShouldRunHourly() throws Exception {
        Scheduled schedule = scheduled(RefreshTokenService.class, "cleanupExpiredTokens");

        assertEquals("0 0 * * * *", schedule.cron());
    }

    private Scheduled scheduled(Class<?> type, String methodName) throws NoSuchMethodException {
        Method method = type.getDeclaredMethod(methodName);
        Scheduled schedule = method.getAnnotation(Scheduled.class);
        assertNotNull(schedule, type.getSimpleName() + "." + methodName + " should be scheduled");
        return schedule;
    }
}
