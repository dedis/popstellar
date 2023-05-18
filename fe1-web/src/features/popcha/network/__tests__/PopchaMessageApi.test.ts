import { mockLaoId, mockPopToken } from '__tests__/utils';
import { publish as mockPublish } from 'core/network';
import { Base64UrlData, Hash, PopToken } from 'core/objects';

import { sendPopchaAuthRequest } from '../PopchaMessageApi';

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
      client_id: mockClientId.toString(),
      nonce: mockNonce,
      identifier: mockPopToken.publicKey,
      popcha_address: mockPopchaAddress,
      state: mockState,
      response_mode: mockResponseMode,
    });
    expect(mockPopToken.sign(new Base64UrlData(mockNonce))).toEqual(message.identifier_proof);
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
      client_id: mockClientId.toString(),
      nonce: mockNonce,
      identifier: mockPopToken.publicKey,
      popcha_address: mockPopchaAddress,
    });
  });
});
