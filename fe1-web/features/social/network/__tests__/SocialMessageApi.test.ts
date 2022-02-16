import 'jest-extended';
import '__tests__/utils/matchers';

import testKeyPair from 'test_data/keypair.json';
import { ActionType, MessageData, ObjectType } from 'model/network/method/message/data';
import { Hash, PublicKey } from 'model/objects';
import { OpenedLaoStore } from 'store';
import { publish as mockPublish } from 'network/JsonRpcApi';
import { mockLao, mockLaoId } from '__tests__/utils/TestUtils';

import { AddChirp } from '../messages/chirp';
import * as msApi from '../SocialMessageApi';

jest.mock('network/JsonRpcApi');
const publishMock = mockPublish as jest.Mock;

const mockText = 'text';

let checkDataAddChirp: Function;

const initializeChecks = () => {
  checkDataAddChirp = (obj: MessageData) => {
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
};

beforeEach(() => {
  publishMock.mockClear();
  OpenedLaoStore.store(mockLao);
  initializeChecks();
});

describe('MessageApi', () => {
  it('should create the correct request for requestAddChirp with parentId', async () => {
    const parentId = new Hash('id');
    await msApi.requestAddChirp(new PublicKey(testKeyPair.publicKey), mockText, parentId);

    expect(publishMock).toBeCalledTimes(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${mockLaoId}/social/${testKeyPair.publicKey}`);
    checkDataAddChirp(msgData);
  });

  it('should create the correct request for requestAddChirp without parentId',
    async () => {
      await msApi.requestAddChirp(new PublicKey(testKeyPair.publicKey), mockText);

      expect(publishMock).toBeCalledTimes(1);
      const [channel, msgData] = publishMock.mock.calls[0];
      expect(channel).toBe(`/root/${mockLaoId}/social/${testKeyPair.publicKey}`);
      checkDataAddChirp(msgData);
    });
});
