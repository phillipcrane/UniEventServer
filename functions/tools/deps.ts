import {
  FacebookService,
  SecretManagerService,
  StorageService,
  DataStoreService,
} from '../src/services';
import type { Dependencies } from '../src/utils';

export function buildDeps(): Dependencies {
  const facebookService = new FacebookService();
  const secretManagerService = new SecretManagerService();
  const storageService = new StorageService();
  const dataStoreService = new DataStoreService();

  return { facebookService, secretManagerService, storageService, dataStoreService };
}