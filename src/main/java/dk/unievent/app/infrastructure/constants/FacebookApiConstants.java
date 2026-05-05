package dk.unievent.app.infrastructure.constants;

public final class FacebookApiConstants {
    private FacebookApiConstants() {}

    // OAuth
    public static final String OAUTH_URL = "https://www.facebook.com/dialog/oauth";
    public static final String OAUTH_SCOPES = "pages_show_list,pages_read_engagement";
    public static final long STATE_MAX_AGE_SECONDS = 900; // 15 minutes

    // Token exchange
    public static final String GRANT_TYPE_EXCHANGE_TOKEN = "fb_exchange_token";
    public static final int DEFAULT_TOKEN_EXPIRY_SECONDS = 5184000; // 60 days

    // Graph API field lists
    public static final String PAGES_FIELDS = "id,name,access_token";
    public static final String EVENTS_FIELDS =
            "id,name,description,start_time,end_time,place,cover,timezone,is_canceled,is_online,type";
    public static final int EVENTS_LIMIT = 100;

    // HTTP
    public static final String BEARER_PREFIX = "Bearer ";
}
