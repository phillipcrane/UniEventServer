package dk.unievent.app.infrastructure.constants;

public final class SchedulingConstants {
    private SchedulingConstants() {}

    public static final long INGESTION_INTERVAL_MS = 43_200_000L;   // 12 hours
    public static final long INGESTION_INITIAL_DELAY_MS = 60_000L;  // 1 minute

    public static final long TOKEN_REFRESH_INTERVAL_MS = 1_728_000_000L; // 20 days
    public static final long TOKEN_REFRESH_INITIAL_DELAY_MS = 120_000L;  // 2 minutes
}
