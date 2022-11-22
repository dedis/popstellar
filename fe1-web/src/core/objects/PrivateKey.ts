import { sign } from 'tweetnacl';

import { Base64UrlData } from './Base64UrlData';
import { Signature } from './Signature';

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
   * Returns the serialized version of the private key that can for instance be stored
   * in redux stores
   * @returns The serialized private key
   */
  toState(): string {
    return this.toString();
  }
}
