import { sha256 } from 'js-sha256';

import { Base64UrlData } from './Base64UrlData';
import { PublicKey } from './PublicKey';
import { Timestamp } from './Timestamp';

export type HashState = string;

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
  public static fromArray(...data: (string | number | String | Timestamp)[]): Hash {
    const str = data
      .map((item) => {
        let itemStr: string;

        if (typeof item === 'string') {
          itemStr = item;
        } else if (typeof item === 'number') {
          itemStr = item.toString();
        } else if (item instanceof String) {
          itemStr = item.valueOf();
        } else if (item instanceof Timestamp) {
          itemStr = item.valueOf().toString();
        } else {
          throw new Error(`Invalid input to Hash.from(): ${item}`);
        }

        return Hash.computeByteLength(itemStr) + itemStr;
      })
      .join('');

    return Hash.fromString(str);
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

    return new Hash(Base64UrlData.encode(str, 'binary').valueOf());
  }

  public equals(o: Hash): boolean {
    return this.toString() === o.toString();
  }

  /**
   * Creates a hash of a public key respecting the digital cash specification
   * https://github.com/dedis/popstellar/blob/42e18f176a91371f28653da8815d9591c38f2ee6/be2-scala/src/main/scala/ch/epfl/pop/model/objects/PublicKey.scala#L8
   *
   * @param publicKey the public key to hash
   */
  public static fromPublicKey(publicKey: string | PublicKey): Hash {
    const hash = sha256.create();

    // We need the raw public key
    const decodedPK = Base64UrlData.fromBase64(publicKey.valueOf()).toBuffer();

    const intArray = hash.update(decodedPK).array();

    // Taking only the first 20 bytes
    const buff = Buffer.from(intArray.slice(0, 20));

    return new Hash(Base64UrlData.encode(buff, 'binary').valueOf());
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

  /**
   * Returns the primitive value used for representing the Hash,
   * a string
   * If you want to serialize an instance use .toState() instead
   */
  public valueOf(): string {
    return super.valueOf();
  }

  /**
   * Deserializes a previously serializes instance of Hash
   */
  public static fromState(hashState: HashState): Hash {
    return new Hash(hashState);
  }
}
