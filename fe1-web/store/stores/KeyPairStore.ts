import {
  Base64UrlData,
  KeyPair, KeyPairState, PrivateKey, PublicKey,
} from 'model/objects';
import { sign } from 'tweetnacl';
import { encodeBase64 } from 'tweetnacl-util';
import { dispatch, getStore } from '../Storage';
import { getKeyPairState, setKeyPair } from '../reducers';

export namespace KeyPairStore {

  export function store(kp: KeyPair): void {
    dispatch(setKeyPair(kp.toState()));
  }

  export function get(): KeyPair {
    // if keys.publicKey or keys.privateKey is undefined (no key in the
    // storage), then a fresh instance is automatically created
    const keysState: KeyPairState | undefined = getKeyPairState(getStore().getState()).keyPair;

    if (!keysState) {
      // create new pair of keys
      const pair = sign.keyPair();

      const keyPair: KeyPair = new KeyPair({
        publicKey: new PublicKey(Base64UrlData.fromBase64(encodeBase64(pair.publicKey)).valueOf()),
        privateKey: new PrivateKey(
          Base64UrlData.fromBase64(encodeBase64(pair.secretKey)).valueOf(),
        ),
      });

      store(keyPair);
      return keyPair;
    }

    return KeyPair.fromState(keysState);
  }

  export function getPublicKey(): PublicKey {
    return get().publicKey;
  }

  export function getPrivateKey(): PrivateKey {
    return get().privateKey;
  }
}
