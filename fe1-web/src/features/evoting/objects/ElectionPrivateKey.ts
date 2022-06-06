import { curve } from '@dedis/kyber';
import Ed25519Scalar from '@dedis/kyber/curve/edwards25519/scalar';

import { Base64UrlData } from 'core/objects';

const ed25519 = curve.newCurve('edwards25519');

export class ElectionPrivateKey {
  public scalar: Ed25519Scalar;

  constructor(encodedKey: Base64UrlData) {
    const scalar = ed25519.scalar();
    scalar.unmarshalBinary(encodedKey.toBuffer());

    if (!(scalar instanceof Ed25519Scalar)) {
      throw new Error('Election keys are expected to be Ed25519 scalars');
    }

    this.scalar = scalar;
  }

  toString(): string {
    return Base64UrlData.encode(this.scalar.marshalBinary()).valueOf();
  }

  toBase64(): Base64UrlData {
    return Base64UrlData.encode(this.scalar.marshalBinary());
  }

  equals(other: ElectionPrivateKey): boolean {
    // lift the equals from the Ed25519 scalar
    return this.scalar.equals(other.scalar);
  }

  /**
   * Decrypts the passed data using elgamal using the ed25519 curve
   * @param encryptedData The encrypted data to encrypt
   * @returns The decrypted data
   */
  decrypt(encryptedData: Base64UrlData): Buffer {
    // Follows this implementation:
    // https://github.com/dedis/cothority/blob/0299bcd78bab22bde6d6449b1594613987355535/evoting/lib/elgamal.go#L27-L31
    const b = encryptedData.toBuffer();

    const K = ed25519.point();
    K.unmarshalBinary(b.slice(0, 32));

    const C = ed25519.point();
    C.unmarshalBinary(b.slice(32, 64));

    // ElGamal-decrypt the ciphertext (K,C) to reproduce the message.
    // S := cothority.Suite.Point().Mul(private, K) // regenerate shared secret
    const S = ed25519.point().mul(this.scalar, K);

    // return cothority.Suite.Point().Sub(C, S)     // use to un-blind the message

    // an example for extracting an embedding was found here:
    // https://github.com/dedis/cothority/blob/0299bcd78bab22bde6d6449b1594613987355535/external/js/kyber/spec/group/edwards25519/point.spec.ts#L239-L251

    return ed25519.point().sub(C, S).data();
  }
}
