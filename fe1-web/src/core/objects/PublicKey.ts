import { Base64UrlData } from './Base64UrlData';

export type PublicKeyState = string;

export class PublicKey extends Base64UrlData {
  /**
   * Returns the primitive value used for representing the PublicKey,
   * a string
   * If you want to serialize an instance use .toState() instead
   */
  public valueOf(): string {
    return super.valueOf();
  }

  /**
   * Checks if two public key instances are equal.
   * @remark Interestingly enough typescript will complain if we do not define this explicitly
   */
  public equals(o: PublicKey): boolean {
    return super.equals(o);
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
