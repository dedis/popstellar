import { Timestamp } from './Timestamp';
import { Hash } from './Hash';
import { PublicKey } from './PublicKey';

export interface ReactionState {
  id: string;
  sender: string;
  reaction_codepoint: string;
  chirp_id: string;
  time: number;
}

export class Reaction {
  public readonly id: Hash;

  // The sender's public key
  public readonly sender: PublicKey;

  // The codepoint of the reaction
  public readonly reaction_codepoint: string;

  // The chirp_id of the reaction added on
  public readonly chirp_id: Hash;

  // The time when the reaction was added
  public readonly time: Timestamp;

  constructor(obj: Partial<Reaction>) {
    if (obj === undefined || obj === null) {
      throw new Error('Error encountered while creating a reaction object: '
        + 'undefined/null parameters');
    }

    if (obj.id === undefined) {
      throw new Error("Undefined 'id' when creating 'Reaction'");
    }
    if (obj.sender === undefined) {
      throw new Error("Undefined 'sender' when creating 'Reaction'");
    }
    if (obj.reaction_codepoint === undefined) {
      throw new Error("Undefined 'reaction_codepoint' when creating 'Reaction'");
    }
    if (obj.chirp_id === undefined) {
      throw new Error("Undefined 'chirp_id' when creating 'Reaction'");
    }
    if (obj.time === undefined) {
      throw new Error("Undefined 'time' when creating 'Reaction'");
    }

    this.id = obj.id;
    this.sender = obj.sender;
    this.reaction_codepoint = obj.reaction_codepoint;
    this.chirp_id = obj.chirp_id;
    this.time = obj.time;
  }

  /**
   * Creates a Reaction object from a ReactionState object.
   *
   * @param reactionState
   */
  public static fromState(reactionState: ReactionState): Reaction {
    return new Reaction({
      id: new Hash(reactionState.id),
      sender: new PublicKey(reactionState.sender),
      reaction_codepoint: reactionState.reaction_codepoint,
      chirp_id: new Hash(reactionState.chirp_id),
      time: new Timestamp(reactionState.time),
    });
  }

  /**
   * Creates a ReactionState object from the current Reaction object.
   */
  public toState(): ReactionState {
    return JSON.parse(JSON.stringify(this));
  }
}
