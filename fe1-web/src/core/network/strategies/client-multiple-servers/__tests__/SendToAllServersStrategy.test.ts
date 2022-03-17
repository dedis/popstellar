import { describe, expect, it } from '@jest/globals';

import { MockNetworkConnection } from 'core/network/__tests__/MockNetworkConnection';
import { JsonRpcMethod, JsonRpcRequest } from 'core/network/jsonrpc';
import { JsonRpcParams } from 'core/network/jsonrpc/JsonRpcParams';
import { NetworkConnection } from 'core/network/NetworkConnection';

import { sendToAllServersStrategy } from '../SendToAllServersStrategy';

const mockChannelId = 'some channel';
const mockAddress = 'some address';

const mockPayload = new JsonRpcRequest({
  id: 1,
  jsonrpc: '',
  method: JsonRpcMethod.PUBLISH,
  params: new JsonRpcParams({ channel: mockChannelId }),
});

describe('SendToAllServersStrategy', () => {
  it('Should send the payload to all connections', async () => {
    const responses = ['r1', 'r2', 'r3'];

    const c1 = new MockNetworkConnection(mockAddress, true, responses[0]);
    const c2 = new MockNetworkConnection(mockAddress, true, responses[1]);
    const c3 = new MockNetworkConnection(mockAddress, true, responses[2]);
    const mockConnections = [c1, c2, c3];

    await expect(sendToAllServersStrategy(
      mockPayload,
      mockConnections as unknown as NetworkConnection[],
    )).resolves.toEqual(responses);

    // make sure send was called on all connections
    expect(c1.sendPayload).toHaveBeenCalledWith(mockPayload);
    expect(c1.sendPayload).toHaveBeenCalledTimes(1);

    expect(c2.sendPayload).toHaveBeenCalledWith(mockPayload);
    expect(c2.sendPayload).toHaveBeenCalledTimes(1);

    expect(c3.sendPayload).toHaveBeenCalledWith(mockPayload);
    expect(c3.sendPayload).toHaveBeenCalledTimes(1);
  });

  it('Should fail if one connection fails', async () => {
    const responses = ['r1', 'r2', 'r3'];
    const errors = ['error 1', 'error 2', 'error 3'];

    const c1 = new MockNetworkConnection(mockAddress, true, responses[0], errors[0]);
    const c2 = new MockNetworkConnection(mockAddress, true, responses[1], errors[1]);
    const c3 = new MockNetworkConnection(mockAddress, false, responses[2], errors[2]);
    const mockConnections = [c1, c2, c3];

    await expect(sendToAllServersStrategy(
      mockPayload,
      mockConnections as unknown as NetworkConnection[],
    )).rejects.toHaveProperty('message', errors[2]);

    // make sure send was called on all connections
    expect(c1.sendPayload).toHaveBeenCalledWith(mockPayload);
    expect(c1.sendPayload).toHaveBeenCalledTimes(1);

    expect(c2.sendPayload).toHaveBeenCalledWith(mockPayload);
    expect(c2.sendPayload).toHaveBeenCalledTimes(1);

    expect(c3.sendPayload).toHaveBeenCalledWith(mockPayload);
    expect(c3.sendPayload).toHaveBeenCalledTimes(1);
  });
});
