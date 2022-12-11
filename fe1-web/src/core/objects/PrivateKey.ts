import { sign } from 'tweetnacl';

import { Base64UrlData } from './Base64UrlData';
import { Signature } from './Signature';

export type PrivateKeyState = string;

export class PrivateKey extends Base64UrlData {
  /**
   * Sign some base64 data with the private key
   *
   * @param data the data to be signed with the private key
   */
  public sign(data: Base64UrlData): Signature {
    const signature = sign.detached(data.toBuffer(), this.toBuffer());
    return new Signature(Base64UrlData.fromBuffer(Buffer.from(signature)).valueOf());
  }

  /**
   * Deserializes a previously serializes instance of PrivateKey
   */
  public static fromState(keyState: PrivateKeyState): PrivateKey {
    return new PrivateKey(keyState);
  }
}
