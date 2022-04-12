import { describe, expect, it } from '@jest/globals';

import { MockNetworkConnection } from 'core/network/__tests__/MockNetworkConnection';
import { mockJsonRpcPayload } from 'core/network/__tests__/utils';
import { NetworkConnection } from 'core/network/NetworkConnection';
import { NetworkError } from 'core/network/NetworkError';

import { sendToFirstAcceptingServerStrategy } from '../SendToFirstAcceptingServerStrategy';

const mockAddress = 'some address';

describe('SendToFirstAcceptingServerStrategy', () => {
  it('Should send the payload over the first connection if it succeeds', async () => {
    const responses = ['r1', 'r2', 'r3'];

    const c1 = new MockNetworkConnection(mockAddress, true, responses[0]);
    const c2 = new MockNetworkConnection(mockAddress, true, responses[1]);
    const c3 = new MockNetworkConnection(mockAddress, true, responses[2]);
    const mockConnections = [c1, c2, c3];

    await expect(
      sendToFirstAcceptingServerStrategy(
        mockJsonRpcPayload,
        mockConnections as unknown as NetworkConnection[],
      ),
    ).resolves.toEqual([responses[0]]);

    expect(c1.sendPayload).toHaveBeenCalledWith(mockJsonRpcPayload);
    expect(c1.sendPayload).toHaveBeenCalledTimes(1);

    expect(c2.sendPayload).toHaveBeenCalledTimes(0);
    expect(c3.sendPayload).toHaveBeenCalledTimes(0);
  });

  it('Should send the payload only over the second connection if the first fails', async () => {
    const responses = ['r1', 'r2', 'r3'];
    const errors = ['e1', 'e2', 'e3'];

    const c1 = new MockNetworkConnection(mockAddress, false, responses[0], errors[0]);
    const c2 = new MockNetworkConnection(mockAddress, true, responses[1], errors[1]);
    const c3 = new MockNetworkConnection(mockAddress, true, responses[2], errors[2]);
    const mockConnections = [c1, c2, c3];

    await expect(
      sendToFirstAcceptingServerStrategy(
        mockJsonRpcPayload,
        mockConnections as unknown as NetworkConnection[],
      ),
    ).resolves.toEqual([responses[1]]);

    expect(c1.sendPayload).toHaveBeenCalledWith(mockJsonRpcPayload);
    expect(c1.sendPayload).toHaveBeenCalledTimes(1);

    expect(c2.sendPayload).toHaveBeenCalledWith(mockJsonRpcPayload);
    expect(c2.sendPayload).toHaveBeenCalledTimes(1);

    expect(c3.sendPayload).toHaveBeenCalledTimes(0);
  });

  it('Should send the payload over the third connection if the first two fail', async () => {
    const responses = ['r1', 'r2', 'r3'];
    const errors = ['e1', 'e2', 'e3'];

    const c1 = new MockNetworkConnection(mockAddress, false, responses[0], errors[0]);
    const c2 = new MockNetworkConnection(mockAddress, false, responses[1], errors[1]);
    const c3 = new MockNetworkConnection(mockAddress, true, responses[2], errors[2]);
    const mockConnections = [c1, c2, c3];

    await expect(
      sendToFirstAcceptingServerStrategy(
        mockJsonRpcPayload,
        mockConnections as unknown as NetworkConnection[],
      ),
    ).resolves.toEqual([responses[2]]);

    expect(c1.sendPayload).toHaveBeenCalledWith(mockJsonRpcPayload);
    expect(c1.sendPayload).toHaveBeenCalledTimes(1);

    expect(c2.sendPayload).toHaveBeenCalledWith(mockJsonRpcPayload);
    expect(c2.sendPayload).toHaveBeenCalledTimes(1);

    expect(c3.sendPayload).toHaveBeenCalledWith(mockJsonRpcPayload);
    expect(c3.sendPayload).toHaveBeenCalledTimes(1);
  });

  it('Should fail if all connection fails', async () => {
    const responses = ['r1', 'r2', 'r3'];
    const errors = ['e1', 'e2', 'e3'];

    const c1 = new MockNetworkConnection(mockAddress, false, responses[0], errors[0]);
    const c2 = new MockNetworkConnection(mockAddress, false, responses[1], errors[1]);
    const c3 = new MockNetworkConnection(mockAddress, false, responses[2], errors[2]);
    const mockConnections = [c1, c2, c3];

    await expect(
      sendToFirstAcceptingServerStrategy(
        mockJsonRpcPayload,
        mockConnections as unknown as NetworkConnection[],
      ),
    ).rejects.toBeInstanceOf(NetworkError);

    expect(c1.sendPayload).toHaveBeenCalledWith(mockJsonRpcPayload);
    expect(c1.sendPayload).toHaveBeenCalledTimes(1);

    expect(c2.sendPayload).toHaveBeenCalledWith(mockJsonRpcPayload);
    expect(c2.sendPayload).toHaveBeenCalledTimes(1);

    expect(c3.sendPayload).toHaveBeenCalledWith(mockJsonRpcPayload);
    expect(c3.sendPayload).toHaveBeenCalledTimes(1);
  });
});
