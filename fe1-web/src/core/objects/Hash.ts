import { sha256 } from 'js-sha256';

import { Base64UrlData } from './Base64Url';

/** Enumeration of all possible event tags used in hash creation */
export enum EventTags {
  MEETING = 'M',
  ROLL_CALL = 'R',
  ELECTION = 'Election',
  QUESTION = 'Question',
  VOTE = 'Vote',
}

export class Hash extends Base64UrlData {
  /**
   * Create a base64url encoded hash of an array of strings according to the
   * communication protocol
   *
   * @param data values to be hashed
   * @return resulting hash
   */
  public static fromStringArray(...data: string[]): Hash {
    const str = data.map((item) => Hash.computeByteLength(item) + item).join('');

    return this.fromString(str);
  }

  /**
   * Create a hash of a string
   *
   * @param data value to be hashed
   * @return resulting hash
   */
  public static fromString(data: string): Hash {
    const hash = sha256.create();

    const bString = hash.update(data).array();
    const str = String.fromCharCode(...bString);

    return Base64UrlData.encode(str, 'binary');
  }

  public equals(o: Hash): boolean {
    return this.toString() === o.toString();
  }

  /**
   * Computes the byte-length of a UTF-8 string
   * @param str string whose length is to be measured
   * @private
   *
   * @remarks
   * This value is necessary as part of the hashing algorithm,
   * and naive `.length` usage reports the number of unicode code points.
   */
  private static computeByteLength(str: string): number {
    const byteArray = new TextEncoder().encode(str);
    return byteArray.length;
  }
}
