/* eslint-disable @typescript-eslint/dot-notation */
import { expect } from '@jest/globals';
import { NetInfoState } from '@react-native-community/netinfo';

import { mockAddress } from '__tests__/utils';

import { getNetworkManager } from '../NetworkManager';
import { SendingStrategy } from '../strategies/ClientMultipleServerStrategy';
import { mockJsonRpcPayload } from './utils';

jest.mock('websocket');

const networkManager = getNetworkManager();
type NetworkManagerType = typeof networkManager;

const NetworkManagerMock = jest.requireMock('core/network/NetworkManager.ts') as {
  getMockNetworkManager: (sendingStrategy: SendingStrategy) => NetworkManagerType;
};

const { getMockNetworkManager } = NetworkManagerMock;

afterEach(() => {
  networkManager.disconnectFromAll();
  networkManager.removeAllReconnectionHandler();
  networkManager.removeAllConnectionDeathHandlers();
});

describe('NetworkManager', () => {
  it('getNetworkManager creates a singleton class', () => {
    const instance1 = getNetworkManager();
    const instance2 = getNetworkManager();
    const instance3 = getNetworkManager();

    expect(networkManager).toBe(instance1);
    expect(instance1).toBe(instance2);
    expect(instance2).toBe(instance3);
  });

  it('can connect to address', async () => {
    const connection = await networkManager.connect(mockAddress);

    // check whether the address of the connection has been set
    expect(connection.address).toEqual(mockAddress);
    // check whether the connection has been added
    expect(networkManager['connections']).toEqual([connection]);
  });

  it('does not create two connections to the same address', async () => {
    const connection1 = await networkManager.connect(mockAddress);
    const connection2 = await networkManager.connect(mockAddress);

    expect(connection1).toBe(connection2);
    expect(networkManager['connections']).toEqual([connection1]);
  });

  it('can connect to multiple addresses', async () => {
    const connection1 = await networkManager.connect(mockAddress);
    const connection2 = await networkManager.connect('wss://some-other-address.com:8000/');

    // check whether the connections have been added
    expect(networkManager['connections']).toEqual([connection1, connection2]);
  });

  it('can disconnect a given connection', async () => {
    const connection = await networkManager.connect(mockAddress);
    networkManager.disconnect(connection);

    expect(networkManager['connections']).toEqual([]);
  });

  it('can disconnect from a given address', async () => {
    const connection = await networkManager.connect(mockAddress);

    expect(networkManager['connections']).toEqual([connection]);

    networkManager.disconnectFrom(mockAddress);

    expect(networkManager['connections']).toEqual([]);
  });

  it('can disconnect from all connections', async () => {
    const connection1 = await networkManager.connect(mockAddress);
    const connection2 = await networkManager.connect('wss://some-other-address.com:8000/');
    const connection3 = await networkManager.connect('wss://another-address.com:8000/');

    expect(networkManager['connections']).toEqual([connection1, connection2, connection3]);

    networkManager.disconnectFromAll();

    expect(networkManager['connections']).toEqual([]);
  });

  it('sets the RPC handler correctly', async () => {
    const connection1 = await networkManager.connect(mockAddress);
    const connection2 = await networkManager.connect('wss://some-other-address.com:8000/');
    const connection3 = await networkManager.connect('wss://another-address.com:8000/');

    const rpcHandler = jest.fn();

    networkManager.setRpcHandler(rpcHandler);

    // ensure the rpc handler of the network manager has been set
    expect(networkManager['rpcHandler']).toBe(rpcHandler);

    // ensure the rpc handler for all connections has been set
    expect(connection1['onRpcHandler']).toBe(rpcHandler);
    expect(connection2['onRpcHandler']).toBe(rpcHandler);
    expect(connection3['onRpcHandler']).toBe(rpcHandler);
  });

  it('can sends data using the sending strategy', async () => {
    const sendingStrategy = jest.fn();
    const mockNetworkManager: NetworkManagerType = getMockNetworkManager(sendingStrategy);

    const connection = await mockNetworkManager.connect(mockAddress);
    mockNetworkManager.sendPayload(mockJsonRpcPayload);

    expect(sendingStrategy).toHaveBeenCalledWith(mockJsonRpcPayload, [connection]);
  });

  it('is possible to add a reconnection handler', () => {
    const handler = jest.fn();
    const handler2 = jest.fn();

    networkManager.addReconnectionHandler(handler);
    expect(networkManager['reconnectionHandlers']).toEqual([handler]);

    networkManager.addReconnectionHandler(handler2);
    expect(networkManager['reconnectionHandlers']).toEqual([handler, handler2]);
  });

  it('is possible to remove a reconnection handler', () => {
    const handler = jest.fn();

    networkManager.addReconnectionHandler(handler);
    networkManager.removeReconnectionHandler(handler);

    expect(networkManager['reconnectionHandlers']).toEqual([]);
  });

  it('is possible to remove all reconnection handlers', () => {
    const handler = jest.fn();
    const handler2 = jest.fn();

    networkManager.addReconnectionHandler(handler);
    networkManager.addReconnectionHandler(handler2);
    networkManager.removeAllReconnectionHandler();

    expect(networkManager['reconnectionHandlers']).toEqual([]);
  });

  it('correctly calls the reconnection handlers', async () => {
    // have to add some connection, otherwise no reconnection happens
    await networkManager.connect(mockAddress);

    const handler = jest.fn();
    const handler2 = jest.fn();

    networkManager.addReconnectionHandler(handler);
    networkManager.addReconnectionHandler(handler2);

    await networkManager['reconnectIfNecessary']();

    expect(handler).toHaveBeenCalledTimes(1);
    expect(handler2).toHaveBeenCalledTimes(1);
  });

  it('is possible to add a connection death handler', () => {
    const handler = jest.fn();
    const handler2 = jest.fn();

    networkManager.addConnectionDeathHandler(handler);
    expect(networkManager['connectionDeathHandlers']).toEqual([handler]);

    networkManager.addConnectionDeathHandler(handler2);
    expect(networkManager['connectionDeathHandlers']).toEqual([handler, handler2]);
  });

  it('is possible to remove a connection death handler', () => {
    const handler = jest.fn();

    networkManager.addConnectionDeathHandler(handler);
    networkManager.removeConnectionDeathHandler(handler);

    expect(networkManager['connectionDeathHandlers']).toEqual([]);
  });

  it('is possible to remove all connection death handlers', () => {
    const handler = jest.fn();
    const handler2 = jest.fn();

    networkManager.addConnectionDeathHandler(handler);
    networkManager.addConnectionDeathHandler(handler2);
    networkManager.removeAllConnectionDeathHandlers();

    expect(networkManager['connectionDeathHandlers']).toEqual([]);
  });

  it('correctly calls the connection death handlers', async () => {
    // have to add some connection, otherwise no reconnection happens
    await networkManager.connect(mockAddress);

    const handler = jest.fn();
    const handler2 = jest.fn();

    networkManager.addConnectionDeathHandler(handler);
    networkManager.addConnectionDeathHandler(handler2);

    networkManager['onConnectionDeath'](mockAddress);

    expect(handler).toHaveBeenCalledTimes(1);
    expect(handler).toHaveBeenCalledWith(mockAddress);

    expect(handler2).toHaveBeenCalledTimes(1);
    expect(handler2).toHaveBeenCalledWith(mockAddress);
  });

  it('triggers reconnect() after being reconnected to the network', async () => {
    // have to add some connection, otherwise no reconnection happens
    await networkManager.connect(mockAddress);

    // mock disconnection
    networkManager['onNetworkChange']({ isConnected: false } as NetInfoState);

    // add reconnection handler
    const handler = jest.fn();
    networkManager.addReconnectionHandler(handler);

    // mock reconnection
    await networkManager['onNetworkChange']({ isConnected: true } as NetInfoState);

    // make sure the handler has been called
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it('triggers reconnect() after becoming active again', async () => {
    // have to add some connection, otherwise no reconnection happens
    await networkManager.connect(mockAddress);

    // mock backgrounding
    networkManager['onAppStateChange']('background');

    // add reconnection handler
    const handler = jest.fn();
    networkManager.addReconnectionHandler(handler);

    // mock becoming active again
    await networkManager['onAppStateChange']('active');

    // make sure the handler has been called
    expect(handler).toHaveBeenCalledTimes(1);
  });
});
