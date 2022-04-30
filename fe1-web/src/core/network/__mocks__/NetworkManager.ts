import { SendingStrategy } from '../strategies/ClientMultipleServerStrategy';

const NetworkManagerModule = jest.requireActual('../NetworkManager');

// expose the correct API
export const { getNetworkManager } = NetworkManagerModule;

// and also one that allows changing the used strategy
const { NetworkManager } = NetworkManagerModule.TEST_ONLY_EXPORTS;
export const getMockNetworkManager = (strategy: SendingStrategy) => new NetworkManager(strategy);
