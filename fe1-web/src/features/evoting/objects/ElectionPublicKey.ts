import { curve, Point } from '@dedis/kyber';

import { Base64UrlData, Base64UrlDataState } from 'core/objects';

const ed25519 = curve.newCurve('edwards25519');

export type ElectionPublicKeyState = Base64UrlDataState;

export class ElectionPublicKey {
  public point: Point;

  constructor(encodedKey: Base64UrlData) {
    const point = ed25519.point();
    point.unmarshalBinary(encodedKey.toBuffer());

    this.point = point;
  }

  toBase64(): Base64UrlData {
    return Base64UrlData.encode(this.point.marshalBinary());
  }

  equals(other: ElectionPublicKey): boolean {
    // lift the equals from the Ed25519 point
    return this.point.equals(other.point);
  }

  /**
   * Encrypts the passed data using elgamal using the ed25519 curve
   * @param data The data to encrypt
   * @returns The points K and C (each 32 bits) appended together and encoded in base64
   */
  encrypt(data: Buffer): Base64UrlData {
    // Follows this implementation:
    // https://github.com/dedis/cothority/blob/0299bcd78bab22bde6d6449b1594613987355535/evoting/lib/elgamal.go#L15-L23

    // an example for embedding was found here:
    // https://github.com/dedis/cothority/blob/0299bcd78bab22bde6d6449b1594613987355535/external/js/kyber/spec/group/edwards25519/point.spec.ts#L203

    // M := cothority.Suite.Point().Embed(message, random.New())
    const M = ed25519.point().embed(data);

    // ElGamal-encrypt the point to produce ciphertext (K,C).
    // k := cothority.Suite.Scalar().Pick(random.New()) // ephemeral private key
    const k = ed25519.scalar().pick();
    // K = cothority.Suite.Point().Mul(k, nil)          // ephemeral DH public key
    const K = ed25519.point().base().mul(k, undefined);
    // S := cothority.Suite.Point().Mul(k, public)      // ephemeral DH shared secret
    const S = ed25519.point().mul(k, this.point);
    // C = S.Add(S, M)                                  // message blinded with secret
    const C = S.add(S, M);

    return Base64UrlData.encode(Buffer.concat([K.marshalBinary(), C.marshalBinary()]));
  }

  /**
   * Returns *some* string representation of this object.
   * If you want to serialize an instance use .toState() instead
   */
  toString(): string {
    return this.toState();
  }

  /**
   * Returns the serialized version of the base64url that can for instance be stored
   * in redux stores
   */
  public toState(): ElectionPublicKeyState {
    return Base64UrlData.encode(this.point.marshalBinary()).valueOf();
  }

  /**
   * Deserializes a previously serializes instance of Base64Url
   */
  public static fromState(electionPublicKeyState: ElectionPublicKeyState): ElectionPublicKey {
    return new ElectionPublicKey(Base64UrlData.fromState(electionPublicKeyState));
  }
}
