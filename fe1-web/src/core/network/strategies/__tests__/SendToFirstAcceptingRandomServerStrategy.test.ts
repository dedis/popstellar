import { describe, expect, it } from '@jest/globals';

import * as arrayFunctions from 'core/functions/Array';
import { MockNetworkConnection } from 'core/network/__tests__/MockNetworkConnection';
import { mockJsonRpcPayload } from 'core/network/__tests__/utils';
import { NetworkConnection } from 'core/network/NetworkConnection';

import { sendToFirstAcceptingRandomServerStrategy } from '../SendToFirstAcceptingRandomServerStrategy';

const mockAddress = 'some address';

const shuffleArray = jest.spyOn(arrayFunctions, 'shuffleArray');

afterEach(() => {
  jest.clearAllMocks();
});
afterAll(() => {
  jest.restoreAllMocks();
});

describe('SendToFirstAcceptingRandomServerStrategy', () => {
  it('Should shuffle the connections before sending the payload', async () => {
    const responses = ['r1', 'r2', 'r3'];

    const c1 = new MockNetworkConnection(mockAddress, true, responses[0]);
    const c2 = new MockNetworkConnection(mockAddress, true, responses[1]);
    const c3 = new MockNetworkConnection(mockAddress, true, responses[2]);
    const mockConnections = [c1, c2, c3];

    expect(shuffleArray).toHaveBeenCalledTimes(0);

    await expect(
      sendToFirstAcceptingRandomServerStrategy(
        mockJsonRpcPayload,
        mockConnections as unknown as NetworkConnection[],
      ),
    ).resolves.toEqual(expect.anything());

    expect(shuffleArray).toHaveBeenCalledWith(mockConnections);
    expect(shuffleArray).toHaveBeenCalledTimes(1);
  });
});
