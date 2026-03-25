"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.RemoteStorageService = void 0;
const axios_1 = __importDefault(require("axios"));
const utils_1 = require("../utils");
class RemoteStorageService {
    http;
    constructor(baseUrl) {
        this.http = axios_1.default.create({
            baseURL: baseUrl ?? utils_1.config.services.storageUrl,
            timeout: 10000,
        });
    }
    async addImage(filePath, data, contentType) {
        const { data: payload } = await this.http.post('/api/storage/images', data, {
            params: { filePath, contentType },
            headers: { 'Content-Type': 'application/octet-stream' },
        });
        return payload?.mediaLink || payload?.filePath || filePath;
    }
    async addImageFromUrl(filePath, sourceUrl) {
        const { data } = await this.http.post('/api/storage/images/from-url', { filePath, sourceUrl });
        return data?.mediaLink || data?.filePath || sourceUrl;
    }
    async getImage(filePath) {
        try {
            const { data } = await this.http.get(`/api/storage/images/${filePath}`);
            if (data?.exists === false) {
                return null;
            }
            return data?.mediaLink || null;
        }
        catch (error) {
            if (error?.response?.status === 404) {
                return null;
            }
            throw error;
        }
    }
    async removeImage(filePath) {
        await this.http.delete(`/api/storage/images/${filePath}`);
    }
}
exports.RemoteStorageService = RemoteStorageService;
