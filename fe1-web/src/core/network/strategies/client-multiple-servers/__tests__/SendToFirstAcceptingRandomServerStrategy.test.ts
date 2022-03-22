import { describe, expect, it } from '@jest/globals';

import * as arrayFunctions from 'core/functions/arrays';
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

    // make sure shuffleArray was called with the correct input
    // .calls[i][j] is the j-th argument of the i-th call
    // converting the two operands to sets since shuffleArray() shuffles in-place
    // and thus .hasBeenCalledWith() reports a wrong order
    expect(new Set(shuffleArray.mock.calls[0][0])).toEqual(new Set(mockConnections));
    expect(shuffleArray).toHaveBeenCalledTimes(1);
  });
});
