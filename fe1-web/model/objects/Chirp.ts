import { Timestamp } from './Timestamp';
import { Hash } from './Hash';

/**
 * Object to represent a Chirp.
 */

export interface ChirpState {
  id: string;
  sender: string;
  text: string;
  time: number;
  likes: number;
  parentId: string;
}

export class Chirp {
  public readonly id: Hash;

  public readonly sender: string;

  public readonly text: string;

  public readonly time: Timestamp;

  public readonly likes: number;

  public readonly parentId?: Hash;

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
    if (obj.text === undefined) {
      throw new Error("Undefined 'text' when creating 'Chirp'");
    }
    if (obj.time === undefined) {
      throw new Error("Undefined 'id' when creating 'Chirp'");
    }
    if (obj.likes === undefined) {
      throw new Error("Undefined 'likes' when creating 'Chirp'");
    }

    this.id = obj.id;
    this.sender = obj.sender;
    this.text = obj.text;
    this.time = obj.time;
    this.likes = obj.likes;
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
      sender: chirpState.sender,
      text: chirpState.text,
      time: new Timestamp(chirpState.time),
      likes: chirpState.likes,
      parentId: new Hash(chirpState.parentId),
    });
  }

  /**
   * Creates a ChirpState object from the current Chirp object.
   */
  public toState(): ChirpState {
    return JSON.parse(JSON.stringify(this));
  }
}
