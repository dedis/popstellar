import { Timestamp } from './Timestamp';
import { Hash } from './Hash';

/**
 * Object to represent a Chirp.
 */

export interface ChirpState {
  sender: string;
  text: string;
  time: number;
  likes: number;
  dislikes: number;
  parentId: string;
}

export class Chirp {
  public readonly sender: string;

  public readonly text: string;

  public readonly time: Timestamp;

  public readonly likes: number;

  public readonly dislikes: number;

  public readonly parentId?: Hash;

  constructor(obj: Partial<Chirp>) {
    if (obj === undefined || obj === null) {
      throw new Error('Error encountered while creating a Chirp object: '
        + 'undefined/null parameters');
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
    if (obj.dislikes === undefined) {
      throw new Error("Undefined 'dislikes' when creating 'Chirp'");
    }

    this.sender = obj.sender;
    this.text = obj.text;
    this.time = obj.time;
    this.likes = obj.likes;
    this.dislikes = obj.dislikes;
    this.parentId = obj.parentId;
  }

  /**
   * Creates a Chirp object from a ChirpState object.
   *
   * @param chirpState
   */
  public static fromState(chirpState: ChirpState): Chirp {
    return new Chirp({
      sender: chirpState.sender,
      text: chirpState.text,
      time: new Timestamp(chirpState.time),
      likes: chirpState.likes,
      dislikes: chirpState.dislikes,
      parentId: new Hash(chirpState.parentId),
    });
  }

  /**
   * Creates a ChirpState object from the current Chirp object.
   */
  public toState(): ChirpState {
    const obj: any = JSON.parse(JSON.stringify(this));
    return { ...obj };
  }
}
