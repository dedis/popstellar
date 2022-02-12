import 'jest-extended';
import {
  Base64UrlData,
  EventTags,
  Hash,
  KeyPair,
  PrivateKey,
  PublicKey,
  Timestamp,
} from 'model/objects';
import { KeyPairStore } from 'store';
import {
  mockLao, mockLaoId, mockPopToken, mockPrivateKey, mockPublicKey,
} from '__tests__/utils/TestUtils';
import { AddChirp, encodeMessageData, EndElection } from '../data';
import { Message } from '../Message';

jest.mock('model/objects/wallet/Token.ts', () => ({
  getCurrentPopTokenFromStore: jest.fn(() => Promise.resolve(mockPopToken)),
}));

const pastKeyPairStoreState = KeyPairStore.get();

beforeAll(() => {
  KeyPairStore.store(KeyPair.fromState({
    publicKey: mockPublicKey,
    privateKey: mockPrivateKey,
  }));
});

describe('Message', () => {
  it('fromData signs the message correctly when adding a chirp', async () => {
    const messageData = new AddChirp({
      text: 'text',
      timestamp: new Timestamp(1607277600),
    });
    const encodedDataJson: Base64UrlData = encodeMessageData(messageData);
    const signature = mockPopToken.privateKey.sign(encodedDataJson);
    const m = await Message.fromData(messageData);
    expect(m.sender).toEqual(mockPopToken.publicKey);
    expect(m.signature).toEqual(signature);
  });

  it('fromData signs the message correctly when ending an election', async () => {
    const messageData = new EndElection({
      lao: mockLao.id,
      election: Hash.fromStringArray(EventTags.ELECTION, mockLaoId, '5678', '1607277600'),
      created_at: new Timestamp(1607277600),
      registered_votes: new Hash('1234'),
    });
    const encodedDataJson: Base64UrlData = encodeMessageData(messageData);
    const privateKey = new PrivateKey(mockPrivateKey);
    const publicKey = new PublicKey(mockPublicKey);
    const signature = privateKey.sign(encodedDataJson);
    const m = await Message.fromData(messageData);
    expect(m.sender).toEqual(publicKey);
    expect(m.signature).toEqual(signature);
  });
});

afterAll(() => {
  KeyPairStore.store(pastKeyPairStoreState);
});
