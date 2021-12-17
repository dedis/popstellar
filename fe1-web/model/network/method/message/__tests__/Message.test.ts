import 'jest-extended';
import testKeyPair from 'test_data/keypair.json';
import { Timestamp } from 'model/objects';
import { AddChirp } from '../data';
import { Message } from '../Message';

jest.mock('store/stores/KeyPairStore.ts', () => ({
  getPublicKey: jest.fn(() => testKeyPair.publicKey),
  getPrivateKey: jest.fn(() => testKeyPair.privateKey),
}));

describe('Message', () => {
  it('fromData signs the message correctly when adding a chirp', () => {
    const message = new AddChirp({
      text: 'text',
      timestamp: new Timestamp(1607277600),
    });
    Message.fromData(message);
  });
});
