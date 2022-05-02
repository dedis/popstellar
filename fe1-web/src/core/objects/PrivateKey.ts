import { sign } from 'tweetnacl';

import { Base64UrlData } from './Base64Url';
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
   * Sign some utf-8 string data with the private key
   *
   * @param data the data to be signed with the private key
   */
  public signUtf8(data: String): Signature {
    const signature = sign.detached(Buffer.from(data, 'utf8'), this.toBuffer());
    return new Signature(Base64UrlData.fromBuffer(Buffer.from(signature)).valueOf());
  }
}
