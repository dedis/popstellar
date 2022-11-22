import base64url from 'base64url';

export type Base64UrlDataState = string;

export class Base64UrlData extends String {
  public constructor(value: string) {
    super(value);

    // raise an exception if it's not base64 data
    base64url.decode(value);
  }

  public static encode(text: string | Buffer, encoding: string = 'utf8'): Base64UrlData {
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
    while (paddedStr.length % 4 !== 0) {
      paddedStr += '=';
    }
    return paddedStr;
  }

  /**
   * Returns *some* string representation of this object.
   * If you need access to the unterlying data type use .valueOf() and
   * if you want to serialize an instance use .toState() instead
   */
  public toString(): string {
    return super.toString();
  }

  /**
   * Returns the primitive value used for representing the Base64UrlData,
   * a string
   * If you want to serialize an instance use .toState() instead
   */
  public valueOf(): string {
    return super.valueOf();
  }

  /**
   * Returns the serialized version of the base64url that can for instance be stored
   * in redux stores
   */
  public toState(): Base64UrlDataState {
    return super.valueOf();
  }

  /**
   * Deserializes a previously serializes instance of Base64Url
   */
  public static fromState(base64UrlDataState: Base64UrlDataState): Base64UrlData {
    return new Base64UrlData(base64UrlDataState);
  }
}
