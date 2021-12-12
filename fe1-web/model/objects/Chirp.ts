import { Timestamp } from './Timestamp';
import { Hash } from './Hash';
import { PublicKey } from './PublicKey';

/**
 * Object to represent a Chirp.
 */

export interface ChirpState {
  id: string;
  sender: string;
  text?: string;
  time: number;
  likes: number;
  parentId?: string;
  isDeleted?: number;
}

export class Chirp {
  public readonly id: Hash;

  // The sender's public key
  public readonly sender: PublicKey;

  // The text of the chirp if it's not deleted
  public readonly text?: string;

  // The time where the chirp was posted
  public readonly time: Timestamp;

  // The number of likes
  public readonly likes: number;

  // The id of the parent chirp (if it is a reply)
  public readonly parentId?: Hash;

  // set to 1 means the chirp is deleted, 0 otherwise
  public readonly isDeleted?: number;

  constructor(obj: Partial<Chirp>) {
    if (obj === undefined || obj === null) {
      throw new Error('Error encountered while creating a Chirp object: '
        + 'undefined/null parameters');
    }

    if (obj.id === undefined) {
      throw new Error("Undefined 'id' when creating 'Chirp'");
    }
    if (obj.sender === undefined) {
      throw new Error("Undefined 'sender' when creating 'Chirp'");
    }
    if (obj.time === undefined) {
      throw new Error("Undefined 'id' when creating 'Chirp'");
    }
    if (obj.likes === undefined) {
      this.likes = 0;
    } else {
      this.likes = obj.likes;
    }
    if (obj.isDeleted === undefined) {
      this.isDeleted = 0;
    } else {
      this.isDeleted = obj.isDeleted;
    }

    this.id = obj.id;
    this.sender = obj.sender;
    this.text = obj.text;
    this.time = obj.time;
    this.parentId = obj.parentId;
  }

  /**
   * Creates a Chirp object from a ChirpState object.
   *
   * @param chirpState
   */
  public static fromState(chirpState: ChirpState): Chirp {
    return new Chirp({
      id: new Hash(chirpState.id),
      sender: new PublicKey(chirpState.sender),
      text: chirpState.text ? chirpState.text : undefined,
      time: new Timestamp(chirpState.time),
      likes: chirpState.likes,
      parentId: chirpState.parentId ? new Hash(chirpState.parentId) : undefined,
      isDeleted: chirpState.isDeleted,
    });
  }

  /**
   * Creates a ChirpState object from the current Chirp object.
   */
  public toState(): ChirpState {
    return JSON.parse(JSON.stringify(this));
  }
}
