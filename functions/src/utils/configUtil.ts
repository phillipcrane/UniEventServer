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
    },
    integration: {
        // When enabled, functions delegates Facebook/Secret/Storage calls to UniEventServer microservices.
        useMicroserviceDal: String(process.env.FUNCTIONS_USE_MICROSERVICE_DAL || '').toLowerCase() === 'true',
    },
    services: {
        facebookUrl: process.env.FACEBOOK_SERVICE_URL || 'http://localhost:8081',
        secretManagerUrl: process.env.SECRET_MANAGER_SERVICE_URL || 'http://localhost:8082',
        storageUrl: process.env.STORAGE_SERVICE_URL || 'http://localhost:8083',
    },
    cors: {
        allowedOrigins: String(process.env.CORS_ALLOWED_ORIGINS || process.env.CLIENT_ORIGIN || '')
            .split(',')
            .map(origin => origin.trim())
            .filter(Boolean),
    }
};
