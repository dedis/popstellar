import 'jest-extended';
import { AnyAction } from 'redux';

import { KeyPair, PrivateKey, PublicKey } from 'core/objects';
import { mockPrivateKey, mockPublicKey } from '__tests__/utils/TestUtils';

import { keyPairReduce, setKeyPair } from '../KeyPairReducer';

const emptyState = {
  keyPair: undefined,
};

const mockKeyPair = new KeyPair({
  publicKey: new PublicKey(mockPublicKey),
  privateKey: new PrivateKey(mockPrivateKey),
}).toState();

const filledState = {
  keyPair: mockKeyPair,
};

describe('KeyPairReducer', () => {
  it('should return the initial state', () => {
    expect(keyPairReduce(undefined, {} as AnyAction)).toEqual(emptyState);
  });

  it('should handle the keyPair being set', () => {
    expect(keyPairReduce({}, setKeyPair(mockKeyPair))).toEqual(filledState);
  });
});
