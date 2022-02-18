import { KeyPair } from './KeyPair';
import { Base64UrlData } from './Base64Url';
import { Signature } from './Signature';

export class PopToken extends KeyPair {
  /**
   * Sign some base64 data with the PoP token
   *
   * @param data the data to be signed with the private key
   */
  public sign(data: Base64UrlData): Signature {
    return this.privateKey.sign(data);
  }
}
