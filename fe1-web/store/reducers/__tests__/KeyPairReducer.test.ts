import 'jest-extended';
import { AnyAction } from 'redux';
import testKeyPair from 'test_data/keypair.json';
import { KeyPair, PrivateKey, PublicKey } from 'model/objects';
import { keyPairReduce, setKeyPair } from '../KeyPairReducer';

const emptyState = {
  keyPair: undefined,
};

const mockPublicKey = new PublicKey(testKeyPair.publicKey);
const mockPrivateKey = new PrivateKey(testKeyPair.privateKey);

const mockKeyPair = new KeyPair({
  publicKey: mockPublicKey,
  privateKey: mockPrivateKey,
}).toState();

const filledState = {
  keyPair: mockKeyPair,
};

describe('KeyPairReducer', () => {
  it('should return the initial state', () => {
    expect(keyPairReduce(undefined, {} as AnyAction))
      .toEqual(emptyState);
  });

  it('should handle the keyPair being set', () => {
    expect(keyPairReduce({}, setKeyPair(mockKeyPair)))
      .toEqual(filledState);
  });
});
