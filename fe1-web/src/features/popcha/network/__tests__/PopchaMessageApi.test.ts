import { mockLaoId, mockPopToken } from '__tests__/utils';
import { publish as mockPublish } from 'core/network';
import { Base64UrlData, PopToken } from 'core/objects';

import { sendPopchaAuthRequest } from '../PopchaMessageApi';

const mockClientId = 'mockClientId';
const mockNonce = 'mockNonce';
const mockEncodedNonce = Base64UrlData.encode(mockNonce);
const mockPopchaAddress = 'mockPopchaAddress';
const mockState = 'mockState';
const mockResponseMode = 'mockResponseMode';

jest.mock('core/network/JsonRpcApi');
const publishMock = mockPublish as jest.Mock;

beforeEach(() => {
  publishMock.mockClear();
});

const mockGenerateToken = (): Promise<PopToken> => {
  return Promise.resolve(mockPopToken);
};

describe('PopchaMessageApi', () => {
  it('should create correct message and publish it', async () => {
    await sendPopchaAuthRequest(
      mockClientId,
      mockNonce,
      mockPopchaAddress,
      mockState,
      mockResponseMode,
      mockLaoId,
      mockGenerateToken,
    );

    expect(publishMock).toHaveBeenCalledTimes(1);
    const [channel, message] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}/authentication`);
    expect(message).toMatchObject({
      client_id: mockClientId,
      nonce: mockEncodedNonce.valueOf(),
      identifier: mockPopToken.publicKey,
      popcha_address: mockPopchaAddress,
      state: mockState,
      response_mode: mockResponseMode,
    });
    expect(mockPopToken.sign(mockEncodedNonce)).toEqual(message.identifier_proof);
  });

  it('should create correct message with null state and response mode', async () => {
    await sendPopchaAuthRequest(
      mockClientId,
      mockNonce,
      mockPopchaAddress,
      null,
      null,
      mockLaoId,
      mockGenerateToken,
    );

    expect(publishMock).toHaveBeenCalledTimes(1);
    const [channel, message] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}/authentication`);
    expect(message).toMatchObject({
      client_id: mockClientId,
      nonce: mockEncodedNonce.valueOf(),
      identifier: mockPopToken.publicKey,
      popcha_address: mockPopchaAddress,
    });
  });
});
