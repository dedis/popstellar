import {
  KeyPair, KeyPairState, PrivateKey, PublicKey,
} from 'model/objects';
import { sign } from 'tweetnacl';
import { encodeBase64 } from 'tweetnacl-util';
import { dispatch, getStore } from '../Storage';
import { ActionKeyPairReducer } from '../Actions';

export namespace KeyPairStore {

  export function store(kp: KeyPair): void {
    dispatch({
      type: ActionKeyPairReducer.SET_KEYPAIR,
      value: kp.toState(),
    });
  }

  export function get(): KeyPair {
    // if keys.publicKey or keys.privateKey is undefined (no key in the
    // storage), then a fresh instance is automatically created
    const keysState: KeyPairState = getStore().getState().keypairReducer;

    if (keysState === null) {
      // create new pair of keys
      const pair = sign.keyPair();

      const keyPair: KeyPair = new KeyPair({
        publicKey: new PublicKey(encodeBase64(pair.publicKey)),
        privateKey: new PrivateKey(encodeBase64(pair.secretKey)),
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
