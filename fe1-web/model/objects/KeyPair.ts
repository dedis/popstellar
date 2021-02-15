import { PublicKey } from './PublicKey';
import { PrivateKey } from './PrivateKey';

export class KeyPair {
  public readonly publicKey: PublicKey;

  public readonly privateKey: PrivateKey;

  constructor(publicKey: PublicKey, privateKey: PrivateKey) {
    this.publicKey = publicKey;
    this.privateKey = privateKey;
  }
}
