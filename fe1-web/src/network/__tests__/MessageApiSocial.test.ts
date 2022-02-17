import 'jest-extended';
import '__tests__/utils/matchers';

import testKeyPair from 'test_data/keypair.json';
import { ActionType, AddChirp, MessageData, ObjectType } from 'model/network/method/message/data';
import { Hash, KeyPair, Lao, PublicKey, Timestamp } from 'model/objects';
import { KeyPairStore, OpenedLaoStore } from 'store';
import * as msApi from '../MessageApi';
import { publish } from '../JsonRpcApi';

jest.mock('network/JsonRpcApi.ts', () => ({
  publish: jest.fn(() => Promise.resolve()),
}));
const lastKeyPair = KeyPairStore.get();
const publishMock = publish as jest.MockedFunction<typeof publish>;
let sampleLao: Lao;

beforeAll(() => {
  KeyPairStore.store(
    KeyPair.fromState({
      publicKey: testKeyPair.publicKey,
      privateKey: testKeyPair.privateKey,
    }),
  );
});

afterAll(() => {
  KeyPairStore.store(lastKeyPair);
});

beforeEach(() => {
  publishMock.mockClear();

  const org = KeyPairStore.getPublicKey();
  const time = Timestamp.EpochNow();
  const name: string = "Pop's LAO";
  sampleLao = new Lao({
    name,
    id: Hash.fromStringArray(org.toString(), time.toString(), name),
    creation: time,
    last_modified: time,
    organizer: org,
    witnesses: [],
  });

  OpenedLaoStore.store(sampleLao);
});

function checkDataAddChirp(obj: MessageData): AddChirp {
  expect(obj.object).toBe(ObjectType.CHIRP);
  expect(obj.action).toBe(ActionType.ADD);

  const data: AddChirp = obj as AddChirp;

  expect(data).toBeObject();
  expect(data.text).toBeString();
  if (data.parent_id) {
    expect(data.parent_id).toBeBase64Url();
  }
  expect(data.timestamp).toBeNumberObject();

  return data;
}

describe('MessageApi', () => {
  it('should create the correct request for requestAddChirp with parentId', async () => {
    const text = 'text';
    const parentId = new Hash('id');

    await msApi.requestAddChirp(new PublicKey(testKeyPair.publicKey), text, parentId);

    expect(publishMock.mock.calls.length).toBe(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${sampleLao.id}/social/${testKeyPair.publicKey}`);

    checkDataAddChirp(msgData);
  });

  it('should create the correct request for requestAddChirp without parentId', async () => {
    const text = 'text';

    await msApi.requestAddChirp(new PublicKey(testKeyPair.publicKey), text);

    expect(publishMock.mock.calls.length).toBe(1);
    const [channel, msgData] = publishMock.mock.calls[0];
    expect(channel).toBe(`/root/${sampleLao.id}/social/${testKeyPair.publicKey}`);

    checkDataAddChirp(msgData);
  });
});
