import base64url from 'base64url';

export class Base64UrlData extends String {
  public constructor(value: string) {
    super(value);

    // raise an exception if it's not base64 data
    base64url.decode(value);
  }

  public static encode(text: string | Buffer, encoding: string = 'utf8') : Base64UrlData {
    return new Base64UrlData(this.addPadding(base64url.encode(text, encoding)));
  }

  public decode(): string {
    return base64url.decode(this.valueOf());
  }

  public equals(o: Base64UrlData): boolean {
    return this.toString() === o.toString();
  }

  public toBuffer(): Buffer {
    return base64url.toBuffer(this.valueOf());
  }

  /**
   * Create Base64Url data from Base64 data
   * @param b64 the base64-encoded data
   */
  public static fromBase64(b64: string): Base64UrlData {
    return new Base64UrlData(this.addPadding(base64url.fromBase64(b64)));
  }

  public static fromBuffer(buf: Buffer): Base64UrlData {
    return new Base64UrlData(this.addPadding(base64url.encode(buf)));
  }

  private static addPadding(str: string): string {
    let paddedStr = str;
    while ((paddedStr.length % 4) !== 0) {
      paddedStr += '=';
    }
    return paddedStr;
  }
}
