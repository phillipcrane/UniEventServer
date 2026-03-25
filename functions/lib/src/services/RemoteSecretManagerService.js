"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.RemoteSecretManagerService = void 0;
const axios_1 = __importDefault(require("axios"));
const utils_1 = require("../utils");
class RemoteSecretManagerService {
    http;
    constructor(baseUrl) {
        this.http = axios_1.default.create({
            baseURL: baseUrl ?? utils_1.config.services.secretManagerUrl,
            timeout: 10000,
        });
    }
    async addPageToken(pageId, token, expiresIn) {
        await this.http.post(`/api/secrets/pages/${pageId}/token`, { token, expiresIn });
    }
    async updatePageToken(pageId, token, expiresIn) {
        await this.http.put(`/api/secrets/pages/${pageId}/token`, { token, expiresIn });
    }
    async getPageToken(pageId) {
        try {
            const { data } = await this.http.get(`/api/secrets/pages/${pageId}/token`);
            return data?.token ?? null;
        }
        catch (error) {
            if (error?.response?.status === 404) {
                return null;
            }
            throw error;
        }
    }
    async checkTokenExpiry(pageId) {
        const { data } = await this.http.get(`/api/secrets/pages/${pageId}/token/status`);
        return Boolean(data?.expired);
    }
    async markTokenExpired(pageId) {
        // UniEventServer secret-manager-service currently has no explicit expire endpoint.
        // Keep this method as a no-op in remote mode to preserve interface compatibility.
        void pageId;
    }
}
exports.RemoteSecretManagerService = RemoteSecretManagerService;
