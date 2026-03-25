"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.StorageService = void 0;
const fs_1 = require("fs");
const path_1 = __importDefault(require("path"));
const node_fetch_1 = __importDefault(require("node-fetch"));
const mime_types_1 = require("mime-types");
class StorageService {
    baseDir;
    constructor(baseDir) {
        this.baseDir = baseDir ?? path_1.default.join(process.cwd(), '.data', 'images');
    }
    async ensureDir() {
        await fs_1.promises.mkdir(this.baseDir, { recursive: true });
    }
    async addImage(filePath, data, contentType) {
        await this.ensureDir();
        const finalPath = path_1.default.join(this.baseDir, filePath);
        await fs_1.promises.mkdir(path_1.default.dirname(finalPath), { recursive: true });
        await fs_1.promises.writeFile(finalPath, data);
        // Return a local path that can later be served by a static file endpoint.
        return `/images/${filePath}`;
    }
    async addImageFromUrl(filePath, sourceUrl) {
        // 1. get the image from the source URL
        const response = await (0, node_fetch_1.default)(sourceUrl);
        if (!response.ok) {
            throw new Error(`Failed to get image from ${sourceUrl}: ${response.statusText}`);
        }
        // 2. determine content type and file extension
        const contentType = response.headers.get('content-type') || 'application/octet-stream';
        // 3. convert response to "buffer", i.e. raw binary data
        const arrayBuffer = await response.arrayBuffer(); // arraybuffer is a generic binary data format
        const buffer = Buffer.from(arrayBuffer);
        // 4. determine file extension from content type
        const ext = (0, mime_types_1.extension)(contentType) || '';
        const finalFilePath = ext ? `${filePath}.${ext}` : filePath;
        return this.addImage(finalFilePath, buffer, contentType);
    }
    async getImage(filePath) {
        const finalPath = path_1.default.join(this.baseDir, filePath);
        try {
            await fs_1.promises.access(finalPath);
        }
        catch {
            return null;
        }
        return `/images/${filePath}`;
    }
    async removeImage(filePath) {
        const finalPath = path_1.default.join(this.baseDir, filePath);
        await fs_1.promises.rm(finalPath, { force: true });
    }
}
exports.StorageService = StorageService;
