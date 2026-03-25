// util for manually configuring app settings. Most settings are via env vars, 
// but some can be manually configured here if needed (e.g. fb api version etc)

export const config = {
    fb: {
        appId: process.env.FACEBOOK_APP_ID || '',
        appSecret: process.env.FACEBOOK_APP_SECRET || '',
        apiVersion: process.env.FACEBOOK_API_VERSION || 'v23.0',
    },
    oauth: {
        redirectUri: process.env.FB_REDIRECT_URI || '',
    },
    token: {
        refreshThresholdDays: 45,
    },
    gcloud: {
        projectId: process.env.GCP_PROJECT_ID || process.env.GOOGLE_CLOUD_PROJECT,
    }
};
