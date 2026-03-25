import { SecretManagerServiceClient } from '@google-cloud/secret-manager';
import { config } from '../utils';
import type { PageToken } from '../types';

export class SecretManagerService {
  // set up Google Secret Manager client (technically just an object that talks to Secret Manager API)
  private client: SecretManagerServiceClient;
  constructor() {
    this.client = new SecretManagerServiceClient();
  }

  
  private getSecretId(pageId: string): string {
    return `facebook-token-${pageId}`;
  }

  async addPageToken(pageId: string, token: string, expiresIn: number): Promise<void> {
    // 1. prepares name and location of secret
    const secretId = this.getSecretId(pageId);
    const projectId = config.gcloud.projectId || process.env.GCP_PROJECT_ID || process.env.GOOGLE_CLOUD_PROJECT;
    if (!projectId) throw new Error('GCP project ID not configured (set GCP_PROJECT_ID/GOOGLE_CLOUD_PROJECT or config.gcloud.projectId)');
    const project = `projects/${projectId}`;
    const secretPath = `${project}/secrets/${secretId}`;

    // 2. calculate expiry date (default to 60 days if not provided, yes, in seconds)
    const expiresInSeconds = expiresIn || 5184000;
    const expiresAt = new Date();
    expiresAt.setSeconds(expiresAt.getSeconds() + expiresInSeconds);

    // 3. prepare "payload", i.e. token to be stored
    const payload: PageToken = {
      token,
      expiresAt: expiresAt.toISOString(),
    };

    try {
      // 4. create secret if it doesn't exist
      await this.client.createSecret({
        parent: project,
        secretId,
        secret: { replication: { automatic: {} } },
      });
    } catch (e: any) {
      // Check both error code (6 = ALREADY_EXISTS) and message
      if (e.code !== 6 && !e.message?.includes('Already exists')) {
        throw e;
      }
    }

    // 5. add new secret version with new token "payload"
    await this.client.addSecretVersion({
      parent: secretPath,
      payload: { data: Buffer.from(token, 'utf8') },
    });
  }

  async updatePageToken(pageId: string, token: string, expiresIn: number): Promise<void> {
    // 1. prepares name and location of secret
    const secretId = this.getSecretId(pageId);
    const projectId = config.gcloud.projectId || process.env.GCP_PROJECT_ID || process.env.GOOGLE_CLOUD_PROJECT;
    if (!projectId) throw new Error('GCP project ID not configured (set GCP_PROJECT_ID/GOOGLE_CLOUD_PROJECT or config.gcloud.projectId)');
    const secretPath = `projects/${projectId}/secrets/${secretId}`;

    // 2. add new secret version with updated token "payload" (token string only)
    await this.client.addSecretVersion({
      parent: secretPath,
      payload: { data: Buffer.from(token, 'utf8') },
    });
  }

  async getPageToken(pageId: string): Promise<string | null> {
    // again, prepares name and location of secret
    const secretId = this.getSecretId(pageId);
    const projectId = config.gcloud.projectId || process.env.GCP_PROJECT_ID || process.env.GOOGLE_CLOUD_PROJECT;
    if (!projectId) throw new Error('GCP project ID not configured (set GCP_PROJECT_ID/GOOGLE_CLOUD_PROJECT or config.gcloud.projectId)');
    const name = `projects/${projectId}/secrets/${secretId}/versions/latest`;

    try {
      // gets latest version of secret, an automatic value stored in Secret Manager
      const [version] = await this.client.accessSecretVersion({ name });
      const payloadString = version.payload?.data 
        ? (typeof version.payload.data === 'string' 
          ? version.payload.data 
          : Buffer.from(version.payload.data).toString('utf8'))
        : null;
      
      return payloadString; // ...else: return token
    } catch (error: any) {
      if (error.code === 5) { // code 5 = not found in gcloud secret manager 
        return null;
      }
      throw error;
    }
  }

  async checkTokenExpiry(pageId: string): Promise<boolean> {
    // For now, tokens don't expire in Secret Manager.
    // Expiry is tracked in page metadata (tokenExpiresAt field).
    return false;
  }

  async markTokenExpired(pageId: string): Promise<void> {
    const secretId = this.getSecretId(pageId);
    const projectId = config.gcloud.projectId || process.env.GCP_PROJECT_ID || process.env.GOOGLE_CLOUD_PROJECT;
    if (!projectId) throw new Error('GCP project ID not configured (set GCP_PROJECT_ID/GOOGLE_CLOUD_PROJECT or config.gcloud.projectId)');
    const name = `projects/${projectId}/secrets/${secretId}`;
    await this.client.deleteSecret({ name }); 
    // actually deletes the secret which we treat as marking it as expired
  }
}
