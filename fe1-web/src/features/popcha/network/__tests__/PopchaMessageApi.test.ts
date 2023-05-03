import { mockLaoId } from '__tests__/utils';
import { publish as mockPublish } from 'core/network';
import { Hash } from 'core/objects';

import { sendPopchaAuthRequest } from '../PopchaMessageApi';
import { generateToken } from '../../../wallet/objects';

const mockClientId = new Hash('mockClientId');
const mockNonce = 'mockNonce';
const mockPopchaAddress = 'mockPopchaAddress';
const mockState = 'mockState';
const mockResponseMode = 'mockResponseMode';

jest.mock('core/network/JsonRpcApi');
const publishMock = mockPublish as jest.Mock;

beforeEach(() => {
  publishMock.mockClear();
});

describe('PopchaMessageApi', () => {
  it('should create correct message and publish it', async () => {
    await sendPopchaAuthRequest(
      mockClientId,
      mockNonce,
      mockPopchaAddress,
      mockState,
      mockResponseMode,
      mockLaoId,
      generateToken,
    );

    expect(publishMock).toHaveBeenCalledTimes(1);
    const [channel, message] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}/authentication`);
  });
});
