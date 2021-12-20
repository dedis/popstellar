import 'jest-extended';
import testKeyPair from 'test_data/keypair.json';
import {
  Base64UrlData,
  EventTags,
  Hash,
  KeyPair,
  Lao,
  LaoState,
  PopToken,
  PrivateKey,
  PublicKey,
  Timestamp,
} from 'model/objects';
import { KeyPairStore, OpenedLaoStore } from 'store';
import { AddChirp, encodeMessageData, OpenRollCall } from '../data';
import { Message } from '../Message';

const TIMESTAMP = 1603455600;
const laoState: LaoState = {
  id: 'LaoID',
  name: 'MyLao',
  creation: TIMESTAMP,
  last_modified: TIMESTAMP,
  organizer: 'organizerPublicKey',
  witnesses: [],
};
const mockLao = Lao.fromState(laoState);

const mockPublicKey = testKeyPair.publicKey;
const mockPrivateKey = testKeyPair.privateKey;
const mockPopToken = PopToken.fromState({
  publicKey: testKeyPair.publicKey2,
  privateKey: testKeyPair.privateKey2,
});
jest.mock('model/objects/wallet/Token.ts', () => ({
  getCurrentPopTokenFromStore: jest.fn(() => Promise.resolve(mockPopToken)),
}));

const pastKeyPairStoreState = KeyPairStore.get();
const pastOpenedLaoStoreState = OpenedLaoStore.get();

beforeAll(() => {
  KeyPairStore.store(KeyPair.fromState({
    publicKey: mockPublicKey,
    privateKey: mockPrivateKey,
  }));
  OpenedLaoStore.store(mockLao);
});

describe('Message', () => {
  it('fromData signs the message correctly when adding a chirp', () => {
    const messageData = new AddChirp({
      text: 'text',
      timestamp: new Timestamp(1607277600),
    });
    const encodedDataJson: Base64UrlData = encodeMessageData(messageData);
    const signature = mockPopToken.privateKey.sign(encodedDataJson);
    Message.fromData(messageData).then((m) => {
      expect(m.sender).toEqual(mockPopToken.publicKey);
      expect(m.signature).toEqual(signature);
    });
  });

  it('fromData signs the message correctly when closing a roll call', () => {
    const messageData = new OpenRollCall({
      update_id: Hash.fromStringArray(EventTags.ROLL_CALL, laoState.id, '5678', '1607277600'),
      opens: new Hash('5678'),
      opened_at: new Timestamp(1607277600),
    });
    const encodedDataJson: Base64UrlData = encodeMessageData(messageData);
    const privateKey = new PrivateKey(mockPrivateKey);
    const publicKey = new PublicKey(mockPrivateKey);
    const signature = privateKey.sign(encodedDataJson);
    Message.fromData(messageData).then((m) => {
      expect(m.sender).toEqual(publicKey);
      expect(m.signature).toEqual(signature);
    });
  });
});

afterAll(() => {
  KeyPairStore.store(pastKeyPairStoreState);
  OpenedLaoStore.store(pastOpenedLaoStoreState);
});
