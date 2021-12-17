import 'jest-extended';
import testKeyPair from 'test_data/keypair.json';
import {
  Base64UrlData, KeyPair,
  PopToken,
  PrivateKey,
  PublicKey,
  Timestamp,
} from 'model/objects';
import { KeyPairStore } from 'store';
import { AddChirp, encodeMessageData } from '../data';
import { Message } from '../Message';

const mockPublicKey = testKeyPair.publicKey;
const mockPrivateKey = testKeyPair.privateKey;
const mockPopToken = PopToken.fromState({
  publicKey: testKeyPair.publicKey2,
  privateKey: testKeyPair.privateKey2,
});
jest.mock('model/objects/wallet/Token.ts', () => ({
  generateToken: jest.fn(async () => mockPopToken),
}));

beforeAll(() => {
  KeyPairStore.store(KeyPair.fromState({
    publicKey: mockPublicKey,
    privateKey: mockPrivateKey,
  }));
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
});
