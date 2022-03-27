import { jest } from '@jest/globals';

import { mockKeyPair } from '__tests__/utils';

import { KeyPair } from '../../objects';

let keyPair: KeyPair = mockKeyPair;

export namespace KeyPairStore {
  export const store = jest.fn<void, [KeyPair]>().mockImplementation((kp: KeyPair) => {
    keyPair = kp;
  });

  export const get = jest.fn<KeyPair, []>().mockImplementation(() => keyPair);

  export const getPublicKey = () => get().publicKey;

  export const getPrivateKey = () => get().privateKey;
}
