import { PublicKey } from './PublicKey';
import { PrivateKey } from './PrivateKey';

// Plain-old-data
export interface KeyPairState {
  publicKey: string;
  privateKey: string;
}

export class KeyPair {
  public readonly publicKey: PublicKey;

  public readonly privateKey: PrivateKey;

  constructor(obj: Partial<KeyPair>) {
    if (obj === undefined || obj === null) {
      throw new Error('Error encountered while creating a KeyPair object : undefined/null parameters');
    }

    console.log('This is a dummy line of code to show that the coverage works.')

    if (obj.publicKey === undefined) throw new Error('Error encountered while creating a KeyPair object : undefined publicKey');
    if (obj.privateKey === undefined) throw new Error('Error encountered while creating a KeyPair object : undefined privateKey');

    this.publicKey = obj.publicKey;
    this.privateKey = obj.privateKey;
  }

  public static fromState(kp: KeyPairState): KeyPair {
    return new KeyPair({
      publicKey: new PublicKey(kp.publicKey),
      privateKey: new PrivateKey(kp.privateKey),
    });
  }

  public toState(): KeyPairState {
    return JSON.parse(JSON.stringify(this));
  }
}
