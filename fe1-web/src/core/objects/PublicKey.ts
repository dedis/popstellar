import { Base64UrlData } from './Base64UrlData';

export type PublicKeyState = string;

export class PublicKey extends Base64UrlData {
  /**
   * Returns *some* string representation of this object.
   * If you need access to the unterlying data type use .valueOf() and
   * if you want to serialize an instance use .toState() instead
   */
  public toString(): string {
    return super.toString();
  }

  /**
   * Returns the primitive value used for representing the PublicKey,
   * a string
   * If you want to serialize an instance use .toState() instead
   */
  public valueOf(): string {
    return super.valueOf();
  }

  /**
   * Returns the serialized version of the public key that can for instance be stored
   * in redux stores
   */
  public toState(): PublicKeyState {
    return super.valueOf();
  }

  /**
   * Deserializes a previously serializes instance of PublicKey
   */
  public static fromState(publicKeyState: PublicKeyState): PublicKey {
    return new PublicKey(publicKeyState);
  }
}
