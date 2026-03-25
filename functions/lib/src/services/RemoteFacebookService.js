"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.RemoteFacebookService = void 0;
const axios_1 = __importDefault(require("axios"));
const utils_1 = require("../utils");
class RemoteFacebookService {
    http;
    constructor(baseUrl) {
        this.http = axios_1.default.create({
            baseURL: baseUrl ?? utils_1.config.services.facebookUrl,
            timeout: 10000,
        });
    }
    async getShortLivedToken(code) {
        const { data } = await this.http.post('/api/facebook/oauth/token', { code });
        return data.access_token;
    }
    async getLongLivedToken(shortLivedToken) {
        const { data } = await this.http.post('/api/facebook/oauth/long-lived-token', { short_lived_token: shortLivedToken });
        return { accessToken: data.access_token, expiresIn: data.expires_in };
    }
    async getPagesFromUser(userAccessToken) {
        const { data } = await this.http.get('/api/facebook/user/pages', { params: { accessToken: userAccessToken } });
        return (data || []).map((page) => ({
            id: page.id,
            name: page.name,
            accessToken: page.access_token,
        }));
    }
    async getPageEvents(pageId, pageAccessToken) {
        const { data } = await this.http.get(`/api/facebook/pages/${pageId}/events`, {
            params: { accessToken: pageAccessToken },
        });
        return data || [];
    }
    async refreshPageToken(pageToken) {
        const { data } = await this.http.post('/api/facebook/oauth/refresh', { page_token: pageToken });
        return { accessToken: data.access_token, expiresIn: data.expires_in };
    }
}
exports.RemoteFacebookService = RemoteFacebookService;
