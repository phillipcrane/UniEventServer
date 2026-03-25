"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.FacebookService = void 0;
const axios_1 = __importDefault(require("axios"));
const utils_1 = require("../utils");
class FacebookService {
    http; // axios is a simple HTTP client (or object) that does HTTP reqs
    constructor() {
        this.http = axios_1.default.create({ baseURL: `https://graph.facebook.com/${utils_1.config.fb.apiVersion}` });
    }
    async getShortLivedToken(code) {
        const { data } = await this.http.get('/oauth/access_token', {
            params: {
                client_id: utils_1.config.fb.appId,
                redirect_uri: utils_1.config.oauth.redirectUri,
                client_secret: utils_1.config.fb.appSecret,
                code,
            },
        });
        return data.access_token;
    }
    async getLongLivedToken(shortLivedToken) {
        const { data } = await this.http.get('/oauth/access_token', {
            params: {
                grant_type: 'fb_exchange_token',
                client_id: utils_1.config.fb.appId,
                client_secret: utils_1.config.fb.appSecret,
                fb_exchange_token: shortLivedToken,
            },
        });
        return { accessToken: data.access_token, expiresIn: data.expires_in };
    }
    async getPagesFromUser(userAccessToken) {
        const { data } = await this.http.get('/me/accounts', {
            params: {
                fields: 'id,name,access_token',
                access_token: userAccessToken,
            },
        });
        return (data?.data || []).map((page) => ({
            id: page.id,
            name: page.name,
            accessToken: page.access_token,
        }));
    }
    async getPageEvents(pageId, pageAccessToken) {
        const { data } = await this.http.get(`/${pageId}/events`, {
            params: {
                time_filter: 'upcoming',
                fields: 'id,name,description,start_time,end_time,place,cover{source}',
                access_token: pageAccessToken,
            },
        });
        return data?.data || [];
    }
    async refreshPageToken(pageToken) {
        // Page tokens are refreshed the same way as any long-lived token
        const { data } = await this.http.get('/oauth/access_token', {
            params: {
                grant_type: 'fb_exchange_token',
                client_id: utils_1.config.fb.appId,
                client_secret: utils_1.config.fb.appSecret,
                fb_exchange_token: pageToken,
            },
        });
        return { accessToken: data.access_token, expiresIn: data.expires_in };
    }
    async refreshLongLivedToken(longLivedToken) {
        return this.refreshPageToken(longLivedToken);
    }
}
exports.FacebookService = FacebookService;
