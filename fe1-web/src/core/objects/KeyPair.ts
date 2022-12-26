import { PrivateKey, PrivateKeyState } from './PrivateKey';
import { PublicKey, PublicKeyState } from './PublicKey';

// Plain-old-data
export interface KeyPairState {
  publicKey: PublicKeyState;
  privateKey: PrivateKeyState;
}

export class KeyPair {
  public readonly publicKey: PublicKey;

  public readonly privateKey: PrivateKey;

  constructor(obj: Partial<KeyPair>) {
    if (obj === undefined || obj === null) {
      throw new Error(
        'Error encountered while creating a KeyPair object : undefined/null parameters',
      );
    }
    if (obj.publicKey === undefined) {
      throw new Error('Error encountered while creating a KeyPair object : undefined publicKey');
    }
    if (obj.privateKey === undefined) {
      throw new Error('Error encountered while creating a KeyPair object : undefined privateKey');
    }

    this.publicKey = obj.publicKey;
    this.privateKey = obj.privateKey;
  }

  public static fromState(kp: KeyPairState): KeyPair {
    return new KeyPair({
      publicKey: PublicKey.fromState(kp.publicKey),
      privateKey: PrivateKey.fromState(kp.privateKey),
    });
  }

  public toState(): KeyPairState {
    return {
      privateKey: this.privateKey.toState(),
      publicKey: this.publicKey.toState(),
    };
  }
}
