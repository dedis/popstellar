import { PublicKey } from './PublicKey';
import { PrivateKey } from './PrivateKey';

export class KeyPair {
  public readonly publicKey: PublicKey;

  public readonly privateKey: PrivateKey;

  constructor(obj: Partial<KeyPair>) {
    if (obj === undefined || obj === null) {
      throw new Error('Error encountered while creating a KeyPair object : undefined/null parameters');
    }

    if (obj.publicKey === undefined) throw new Error('Error encountered while creating a KeyPair object : undefined publicKey');
    if (obj.privateKey === undefined) throw new Error('Error encountered while creating a KeyPair object : undefined privateKey');

    this.publicKey = obj.publicKey;
    this.privateKey = obj.privateKey;
  }
}
