import { promises as fs } from 'fs';
import path from 'path';
import fetch from 'node-fetch';
import { extension } from 'mime-types';

export class StorageService {
  private readonly baseDir: string;

  constructor(baseDir?: string) {
    this.baseDir = baseDir ?? path.join(process.cwd(), '.data', 'images');
  }

  private async ensureDir(): Promise<void> {
    await fs.mkdir(this.baseDir, { recursive: true });
  }

  async addImage(filePath: string, data: Buffer, contentType: string): Promise<string> {
    await this.ensureDir();
    const finalPath = path.join(this.baseDir, filePath);
    await fs.mkdir(path.dirname(finalPath), { recursive: true });
    await fs.writeFile(finalPath, data);

    // Return a local path that can later be served by a static file endpoint.
    return `/images/${filePath}`;
  }

  async addImageFromUrl(filePath: string, sourceUrl: string): Promise<string> {
    // 1. get the image from the source URL
    const response = await fetch(sourceUrl);
    if (!response.ok) {
      throw new Error(`Failed to get image from ${sourceUrl}: ${response.statusText}`);
    }

    // 2. determine content type and file extension
    const contentType = response.headers.get('content-type') || 'application/octet-stream';

    // 3. convert response to "buffer", i.e. raw binary data
    const arrayBuffer = await response.arrayBuffer(); // arraybuffer is a generic binary data format
    const buffer = Buffer.from(arrayBuffer);
    
    // 4. determine file extension from content type
    const ext = extension(contentType) || '';
    const finalFilePath = ext ? `${filePath}.${ext}` : filePath;

    return this.addImage(finalFilePath, buffer, contentType);
  }

  async getImage(filePath: string): Promise<string | null> {
    const finalPath = path.join(this.baseDir, filePath);
    try {
      await fs.access(finalPath);
    } catch {
      return null;
    }

    return `/images/${filePath}`;
  }

  async removeImage(filePath: string): Promise<void> {
    const finalPath = path.join(this.baseDir, filePath);
    await fs.rm(finalPath, { force: true });
  }
}
