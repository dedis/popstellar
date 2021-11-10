import { Timestamp } from './Timestamp';

export interface ChirpState {
  sender: string;
  message: string;
  time: number;
  likes: number;
  dislikes: number;
  replies: number;
}

export class Chirp {
  public readonly sender: string;

  public readonly message: string;

  public readonly time: Timestamp;

  public readonly likes: number;

  public readonly dislikes: number;

  public readonly replies: number;

  constructor(obj: Partial<Chirp>) {
    if (obj === undefined || obj === null) {
      throw new Error('Error encountered while creating a Chirp object: '
        + 'undefined/null parameters');
    }

    if (obj.sender === undefined) {
      throw new Error("Undefined 'sender' when creating 'Chirp'");
    }
    if (obj.message === undefined) {
      throw new Error("Undefined 'message' when creating 'Chirp'");
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
    if (obj.replies === undefined) {
      throw new Error("Undefined 'replies' when creating 'Chirp'");
    }

    this.sender = obj.sender;
    this.message = obj.message;
    this.time = obj.time;
    this.likes = obj.likes;
    this.dislikes = obj.dislikes;
    this.replies = obj.replies;
  }

  public static fromState(c: ChirpState): Chirp {
    return new Chirp({
      sender: c.sender,
      message: c.message,
      time: new Timestamp(c.time),
      likes: c.likes,
      dislikes: c.dislikes,
      replies: c.replies,
    });
  }

  public toState(): ChirpState {
    const obj: any = JSON.parse(JSON.stringify(this));
    return { ...obj };
  }
}
