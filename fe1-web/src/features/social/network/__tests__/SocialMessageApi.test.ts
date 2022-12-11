import 'jest-extended';
import '__tests__/utils/matchers';

import testKeyPair from 'test_data/keypair.json';

import { configureTestFeatures, mockLao, serializedMockLaoId, mockLaoId } from '__tests__/utils';
import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { publish as mockPublish } from 'core/network/JsonRpcApi';
import { Hash, PublicKey } from 'core/objects';
import { OpenedLaoStore } from 'features/lao/store';

import { AddChirp } from '../messages/chirp';
import * as msApi from '../SocialMessageApi';

jest.mock('core/network/JsonRpcApi');
const publishMock = mockPublish as jest.Mock;

const mockText = 'text';

const checkDataAddChirp = (obj: MessageData) => {
  expect(obj.object).toBe(ObjectType.CHIRP);
  expect(obj.action).toBe(ActionType.ADD);

  const data: AddChirp = obj as AddChirp;
  expect(data).toBeObject();
  expect(data.text).toBeString();
  if (data.parent_id) {
    expect(data.parent_id).toBeBase64Url();
  }
  expect(data.timestamp).toBeNumberObject();
};

beforeAll(configureTestFeatures);

beforeEach(() => {
  publishMock.mockClear();
  OpenedLaoStore.store(mockLao);
});

describe('MessageApi', () => {
  it('should create the correct request for requestAddChirp with parentId', async () => {
    const parentId = new Hash('id');
    await msApi.requestAddChirp(
      new PublicKey(testKeyPair.publicKey),
      mockText,
      mockLaoId,
      parentId,
    );

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${serializedMockLaoId}/social/${testKeyPair.publicKey}`);
    checkDataAddChirp(msgData);
  });

  it('should create the correct request for requestAddChirp without parentId', async () => {
    await msApi.requestAddChirp(new PublicKey(testKeyPair.publicKey), mockText, mockLaoId);

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${serializedMockLaoId}/social/${testKeyPair.publicKey}`);
    checkDataAddChirp(msgData);
  });
});
