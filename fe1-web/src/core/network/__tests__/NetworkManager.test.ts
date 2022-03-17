/* eslint-disable @typescript-eslint/dot-notation */
import { expect } from '@jest/globals';

import { getNetworkManager } from '../NetworkManager';
import { mockJsonRpcPayload } from './utils';

jest.mock('websocket');

const networkManager = getNetworkManager();

afterEach(() => {
  networkManager.disconnectFromAll();
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
    // make sure we got a clean network manager instance
    expect(networkManager['connections']).toEqual([]);

    const mockAddress = 'some address';
    const connection = networkManager.connect(mockAddress);

    // check whether the address of the connection has been set
    expect(connection.address).toEqual(mockAddress);
    // check whether the connection has been added
    expect(networkManager['connections']).toEqual([connection]);
  });

  it('does not create two connections to the same address', () => {
    // make sure we got a clean network manager instance
    expect(networkManager['connections']).toEqual([]);

    const mockAddress = 'some address';
    const connection1 = networkManager.connect(mockAddress);
    const connection2 = networkManager.connect(mockAddress);

    expect(connection1).toBe(connection2);
    expect(networkManager['connections']).toEqual([connection1]);
  });

  it('can disconnect a given connection', () => {
    // make sure we got a clean network manager instance
    expect(networkManager['connections']).toEqual([]);

    const mockAddress = 'some address';
    const connection = networkManager.connect(mockAddress);

    expect(networkManager['connections']).toEqual([connection]);

    networkManager.disconnect(connection);

    expect(networkManager['connections']).toEqual([]);
  });

  it('can disconnect from a given address', () => {
    // make sure we got a clean network manager instance
    expect(networkManager['connections']).toEqual([]);

    const mockAddress = 'some address';
    const connection = networkManager.connect(mockAddress);

    expect(networkManager['connections']).toEqual([connection]);

    networkManager.disconnectFrom(mockAddress);

    expect(networkManager['connections']).toEqual([]);
  });

  it('can disconnect from all connections', () => {
    // make sure we got a clean network manager instance
    expect(networkManager['connections']).toEqual([]);

    const connection1 = networkManager.connect('some address');
    const connection2 = networkManager.connect('some other address');
    const connection3 = networkManager.connect('another address');

    expect(networkManager['connections']).toEqual([connection1, connection2, connection3]);

    networkManager.disconnectFromAll();

    expect(networkManager['connections']).toEqual([]);
  });

  it('sets the RPC handler correctly', () => {
    // make sure we got a clean network manager instance
    expect(networkManager['connections']).toEqual([]);

    const connection1 = networkManager.connect('some address');
    const connection2 = networkManager.connect('some other address');
    const connection3 = networkManager.connect('another address');

    expect(networkManager['connections']).toEqual([connection1, connection2, connection3]);

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
    // make sure we got a clean network manager instance
    expect(networkManager['connections']).toEqual([]);

    const connection = networkManager.connect('some address');

    // override sending strategy
    const originalSendingStrategy = networkManager['sendingStrategy'];
    networkManager['sendingStrategy'] = jest.fn();

    networkManager.sendPayload(mockJsonRpcPayload);

    expect(networkManager['sendingStrategy']).toHaveBeenCalledWith(mockJsonRpcPayload, [
      connection,
    ]);

    // restore sending strategy
    networkManager['sendingStrategy'] = originalSendingStrategy;
  });
});
