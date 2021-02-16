import { KeyPair, PrivateKey, PublicKey } from 'model/objects';
import { sign } from 'tweetnacl';
import { encodeBase64 } from 'tweetnacl-util';
import { dispatch, getStore } from '../Storage';
import { ActionKeyPairReducer } from '../Actions';

export namespace KeyPairStore {

  export function store(value: KeyPair): void {
    dispatch({
      type: ActionKeyPairReducer.SET_KEYPAIR,
      value: { publicKey: value.publicKey, privateKey: value.privateKey },
    });
  }

  export function get(): KeyPair {
    // if keys.publicKey or keys.privateKey is undefined (no key in the
    // storage), then a fresh instance is automatically created
    const keys: KeyPair = getStore().getState().keypairReducer;

    if (keys === null) {
      // create new pair of keys
      const pair = sign.keyPair();

      const keyPair: KeyPair = new KeyPair({
        publicKey: new PublicKey(encodeBase64(pair.publicKey)),
        privateKey: new PrivateKey(encodeBase64(pair.secretKey)),
      });

      store(keyPair);
      return keyPair;
    }

    return keys;
  }

  export function getPublicKey(): PublicKey {
    return get().publicKey;
  }

  export function getPrivateKey(): PrivateKey {
    return get().privateKey;
  }
}
