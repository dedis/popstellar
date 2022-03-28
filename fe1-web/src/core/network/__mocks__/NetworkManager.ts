import { SendingStrategy } from '../strategies/ClientMultipleServerStrategy';

const NetworkManagerFile = jest.requireActual('../NetworkManager');

// expose the correct API
export const { getNetworkManager } = NetworkManagerFile;

// and also one that allows changing the used strategy
const { NetworkManager } = NetworkManagerFile.TEST_ONLY_EXPORTS;
export const getMockNetworkManager = (strategy: SendingStrategy) => new NetworkManager(strategy);
