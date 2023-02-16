import * as Random from 'expo-random';
import { sign } from 'tweetnacl';

import { Base64UrlData, KeyPair, KeyPairState, PrivateKey, PublicKey } from '../objects';
import { dispatch, getStore } from '../redux';
import { getKeyPairState, setKeyPair } from './KeyPairReducer';

/**
 * The KeyPairStore stores the unique public/private key pair
 * that uniquely identifies (and de-anonymizes) the user in the PoP system.
 * An organizer's or witness's public key would be stored here.
 *
 * @remarks
 * It should ensure that all data gets stored in an encrypted format,
 * but this is not yet the case and would require some amount of refactoring.
 */
export namespace KeyPairStore {
  /**
   * Stores unique public/private key pair
   * @param kp the key pair
   */
  export function store(kp: KeyPair): void {
    dispatch(setKeyPair(kp.toState()));
  }

  /**
   * Returns the keypair
   * @returns A KeyPair object
   */
  export function get(): KeyPair {
    // if keys.publicKey or keys.privateKey is undefined (no key in the
    // storage), then a fresh instance is automatically created
    const keysState: KeyPairState | undefined = getKeyPairState(getStore().getState()).keyPair;

    if (!keysState) {
      // create new pair of keys

      const pair = sign.keyPair.fromSeed(Random.getRandomBytes(32));

      const keyPair: KeyPair = new KeyPair({
        publicKey: new PublicKey(Base64UrlData.fromBuffer(Buffer.from(pair.publicKey)).valueOf()),
        privateKey: new PrivateKey(Base64UrlData.fromBuffer(Buffer.from(pair.secretKey)).valueOf()),
      });

      store(keyPair);
      return keyPair;
    }

    return KeyPair.fromState(keysState);
  }

  /**
   * Retrieves the public key
   * @returns A PublicKey object
   */
  export function getPublicKey(): PublicKey {
    return get().publicKey;
  }

  /**
   * Retrieves the private key
   * @returns A PrivateKey object
   */
  export function getPrivateKey(): PrivateKey {
    return get().privateKey;
  }
}
