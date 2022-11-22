import { Base64UrlData } from './Base64Url';

export class PublicKey extends Base64UrlData {
  /**
   * Returns the serialized version of the public key that can for instance be stored
   * in redux stores
   * @returns The serialized public key
   */
  serialize(): string {
    return this.toString();
  }
}
