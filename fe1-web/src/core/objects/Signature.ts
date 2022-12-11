import { sign } from 'tweetnacl';

import { Base64UrlData } from './Base64UrlData';
import { PublicKey } from './PublicKey';

export type SignatureState = string;

export class Signature extends Base64UrlData {
  /**
   * Verify the signature for the message data and return true iff verification succeeded
   *
   * @param key public key of the presumed sender
   * @param data base64Url signed message
   * @return true iff the signature verification succeeded
   */
  public verify(key: PublicKey, data: Base64UrlData): boolean {
    try {
      return sign.detached.verify(data.toBuffer(), this.toBuffer(), key.toBuffer());
    } catch {
      return false;
    }
  }

  /**
   * Returns the primitive value used for representing the Signature,
   * a string
   * If you want to serialize an instance use .toState() instead
   */
  public valueOf(): string {
    return super.valueOf();
  }

  /**
   * Returns the serialized version of the signature that can for instance be stored
   * in redux stores
   */
  public toState(): SignatureState {
    return super.valueOf();
  }

  /**
   * Deserializes a previously serializes instance of Signature
   */
  public static fromState(signatureState: SignatureState): Signature {
    return new Signature(signatureState);
  }
}
