/* eslint-disable @typescript-eslint/dot-notation */
import { expect } from '@jest/globals';
import { NetInfoState } from '@react-native-community/netinfo';

import { getNetworkManager } from '../NetworkManager';
import { mockJsonRpcPayload } from './utils';

jest.mock('websocket');

const networkManager = getNetworkManager();

afterEach(() => {
  networkManager.disconnectFromAll();
  networkManager.removeAllReconnectionHandler();
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

  it('can connect to address', () => {
    const mockAddress = 'some address';
    const connection = networkManager.connect(mockAddress);

    // check whether the address of the connection has been set
    expect(connection.address).toEqual(mockAddress);
    // check whether the connection has been added
    expect(networkManager['connections']).toEqual([connection]);
  });

  it('does not create two connections to the same address', () => {
    const mockAddress = 'some address';
    const connection1 = networkManager.connect(mockAddress);
    const connection2 = networkManager.connect(mockAddress);

    expect(connection1).toBe(connection2);
    expect(networkManager['connections']).toEqual([connection1]);
  });

  it('can connect to multiple addresses', () => {
    const connection1 = networkManager.connect('some address');
    const connection2 = networkManager.connect('some other address');

    // check whether the connections have been added
    expect(networkManager['connections']).toEqual([connection1, connection2]);
  });

  it('can disconnect a given connection', () => {
    const connection = networkManager.connect('some address');
    networkManager.disconnect(connection);

    expect(networkManager['connections']).toEqual([]);
  });

  it('can disconnect from a given address', () => {
    const mockAddress = 'some address';
    const connection = networkManager.connect(mockAddress);

    expect(networkManager['connections']).toEqual([connection]);

    networkManager.disconnectFrom(mockAddress);

    expect(networkManager['connections']).toEqual([]);
  });

  it('can disconnect from all connections', () => {
    const connection1 = networkManager.connect('some address');
    const connection2 = networkManager.connect('some other address');
    const connection3 = networkManager.connect('another address');

    expect(networkManager['connections']).toEqual([connection1, connection2, connection3]);

    networkManager.disconnectFromAll();

    expect(networkManager['connections']).toEqual([]);
  });

  it('sets the RPC handler correctly', () => {
    const connection1 = networkManager.connect('some address');
    const connection2 = networkManager.connect('some other address');
    const connection3 = networkManager.connect('another address');

    const rpcHandler = jest.fn();

    networkManager.setRpcHandler(rpcHandler);

    // ensure the rpc handler of the network manager has been set
    expect(networkManager['rpcHandler']).toBe(rpcHandler);

    // ensure the rpc handler for all connections has been set
    expect(connection1['onRpcHandler']).toBe(rpcHandler);
    expect(connection2['onRpcHandler']).toBe(rpcHandler);
    expect(connection3['onRpcHandler']).toBe(rpcHandler);
  });

  it('can sends data using the sending strategy', () => {
    const connection = networkManager.connect('some address');

    // override sending strategy
    const originalSendingStrategy = networkManager['sendingStrategy'];
    // @ts-ignore
    networkManager['sendingStrategy'] = jest.fn();

    networkManager.sendPayload(mockJsonRpcPayload);

    expect(networkManager['sendingStrategy']).toHaveBeenCalledWith(mockJsonRpcPayload, [
      connection,
    ]);

    // restore sending strategy
    // @ts-ignore
    networkManager['sendingStrategy'] = originalSendingStrategy;
  });

  it('is possible to add a reconnection handlers', () => {
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

  it('is possible to remove all reconnection handler', () => {
    const handler = jest.fn();
    const handler2 = jest.fn();

    networkManager.addReconnectionHandler(handler);
    networkManager.addReconnectionHandler(handler2);
    networkManager.removeAllReconnectionHandler();

    expect(networkManager['reconnectionHandlers']).toEqual([]);
  });

  it('correctly calls the reconnection handlers', () => {
    // have to add some connection, otherwise no reconnection happens
    networkManager.connect('some address');

    const handler = jest.fn();
    const handler2 = jest.fn();

    networkManager.addReconnectionHandler(handler);
    networkManager.addReconnectionHandler(handler2);

    networkManager['reconnect']();

    expect(handler).toHaveBeenCalledTimes(1);
    expect(handler2).toHaveBeenCalledTimes(1);
  });

  it('triggers reconnect() after being reconnected to the network', () => {
    // have to add some connection, otherwise no reconnection happens
    networkManager.connect('some address');

    // mock disconnection
    networkManager['onNetworkChange']({ isConnected: false } as NetInfoState);

    // add reconnection handler
    const handler = jest.fn();
    networkManager.addReconnectionHandler(handler);

    // mock reconnection
    networkManager['onNetworkChange']({ isConnected: true } as NetInfoState);

    // make sure the handler has been called
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it('triggers reconnect() after becoming active again', () => {
    // have to add some connection, otherwise no reconnection happens
    networkManager.connect('some address');

    // mock backgrounding
    networkManager['onAppStateChange']('background');

    // add reconnection handler
    const handler = jest.fn();
    networkManager.addReconnectionHandler(handler);

    // mock becoming active again
    networkManager['onAppStateChange']('active');

    // make sure the handler has been called
    expect(handler).toHaveBeenCalledTimes(1);
  });
});
