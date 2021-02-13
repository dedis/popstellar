import { KeyPair, PrivateKey, PublicKey } from '../../Model/Objects';
import { dispatch } from '../Storage';
import { KeyPairData } from '../objects';
import { Store } from "redux";
import { sign } from "tweetnacl";
import { encodeBase64 } from "tweetnacl-util";


export class StorageKeyPair {

  public readonly storage: Store;

  constructor(storage: Store) {
    this.storage = storage;
  }

  public store(value: KeyPair): void {
    dispatch({
      type: 'SET_KEYPAIR',
      value: { publicKey: value.publicKey, privateKey: value.privateKey }
    });
  }

  public getKeyPair(): KeyPair {
    // if keys.publicKey or keys.privateKey is undefined (no key in the
    // storage), then a fresh instance is automatically created
    let keys: KeyPairData = this.storage.getState().keypairReducer;

    if (keys.publicKey === undefined || keys.privateKey === undefined) {
      // create new pair of keys
      const pair = sign.keyPair();

      const keyPair: KeyPair = new KeyPair(
        new PublicKey(encodeBase64(pair.publicKey)),
        new PrivateKey(encodeBase64(pair.secretKey))
      );

      this.store(keyPair);
      return keyPair;
    }

    return new KeyPair(keys.publicKey, keys.privateKey);
  }

  public getPublicKey(): PublicKey {
    return this.getKeyPair().publicKey;
  }

  public getPrivateKey(): PrivateKey {
    return this.getKeyPair().privateKey;
  }
}
