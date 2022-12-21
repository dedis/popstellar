import {
  Hash,
  HashState,
  PublicKey,
  PublicKeyState,
  Timestamp,
  TimestampState,
} from 'core/objects';
import { OmitMethods } from 'core/types';

/**
 * Object to represent a Reaction.
 */

export interface ReactionState {
  id: HashState;
  sender: PublicKeyState;
  codepoint: string;
  chirpId: HashState;
  time: TimestampState;
}

export class Reaction {
  public readonly id: Hash;

  // The sender's public key
  public readonly sender: PublicKey;

  // The codepoint of the reaction
  public readonly codepoint: string;

  // The chirp_id of the reaction added on
  public readonly chirpId: Hash;

  // The time when the reaction was added
  public readonly time: Timestamp;

  constructor(obj: OmitMethods<Reaction>) {
    if (obj === undefined || obj === null) {
      throw new Error(
        'Error encountered while creating a reaction object: undefined/null parameters',
      );
    }

    if (obj.id === undefined) {
      throw new Error("Undefined 'id' when creating 'Reaction'");
    }
    if (obj.sender === undefined) {
      throw new Error("Undefined 'sender' when creating 'Reaction'");
    }
    if (obj.codepoint === undefined) {
      throw new Error("Undefined 'reaction_codepoint' when creating 'Reaction'");
    }
    if (obj.chirpId === undefined) {
      throw new Error("Undefined 'chirp_id' when creating 'Reaction'");
    }
    if (obj.time === undefined) {
      throw new Error("Undefined 'time' when creating 'Reaction'");
    }

    this.id = obj.id;
    this.sender = obj.sender;
    this.codepoint = obj.codepoint;
    this.chirpId = obj.chirpId;
    this.time = obj.time;
  }

  /**
   * Creates a Reaction object from a ReactionState object.
   *
   * @param reactionState
   */
  public static fromState(reactionState: ReactionState): Reaction {
    return new Reaction({
      id: Hash.fromState(reactionState.id),
      sender: PublicKey.fromState(reactionState.sender),
      codepoint: reactionState.codepoint,
      chirpId: Hash.fromState(reactionState.chirpId),
      time: Timestamp.fromState(reactionState.time),
    });
  }

  /**
   * Creates a ReactionState object from the current Reaction object.
   */
  public toState(): ReactionState {
    return {
      id: this.id.toState(),
      sender: this.sender.toState(),
      codepoint: this.codepoint,
      chirpId: this.chirpId.toState(),
      time: this.time.toState(),
    };
  }
}
