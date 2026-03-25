import { DataStoreService, type IFacebookService, type ISecretManagerService, type IStorageService } from '../services';

// Dependency injection = passing objects into functions instead of importing them directly. This means 
// that instead of the functions importing the datastore/Facebook/secret services directly,
// they receive instances as parameters (i.e. inside function(parameters)). Also helps with testing

export type Dependencies = {
    facebookService: IFacebookService;
    secretManagerService: ISecretManagerService;
    storageService: IStorageService;
    dataStoreService: DataStoreService;
};
