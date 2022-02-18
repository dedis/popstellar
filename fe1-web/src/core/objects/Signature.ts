import { sign } from 'tweetnacl';
import { PublicKey } from 'core/objects/PublicKey';
import { Base64UrlData } from 'core/objects/Base64Url';

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
}
