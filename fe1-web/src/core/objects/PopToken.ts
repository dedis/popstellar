import { Base64UrlData } from './Base64UrlData';
import { KeyPair, KeyPairState } from './KeyPair';
import { PrivateKey } from './PrivateKey';
import { PublicKey } from './PublicKey';
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

  public static fromState(kp: KeyPairState): PopToken {
    return new PopToken({
      publicKey: new PublicKey(kp.publicKey),
      privateKey: new PrivateKey(kp.privateKey),
    });
  }
}
