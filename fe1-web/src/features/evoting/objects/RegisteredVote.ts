import {
  Hash,
  HashState,
  ProtocolError,
  PublicKey,
  PublicKeyState,
  Timestamp,
  TimestampState,
} from 'core/objects';
import { OmitMethods } from 'core/types';

import { EncryptedVote, EncryptedVoteState } from './EncryptedVote';
import { Vote, VoteState } from './Vote';

export interface RegisteredVoteState {
  messageId: HashState;
  sender: PublicKeyState;
  votes: VoteState[] | EncryptedVoteState[];
  createdAt: TimestampState;
}

export class RegisteredVote {
  public readonly messageId: Hash;

  public readonly sender: PublicKey;

  public readonly votes: Vote[] | EncryptedVote[];

  public readonly createdAt: Timestamp;

  constructor(registeredVote: OmitMethods<RegisteredVote>) {
    if (!registeredVote) {
      throw new Error(
        'Error encountered while creating a RegisteredVote object: undefined/null parameters',
      );
    }

    if (!registeredVote.messageId) {
      throw new ProtocolError(
        "Undefined 'messageId' parameter encountered during 'RegisteredVote'",
      );
    }
    this.messageId = registeredVote.messageId;

    if (!registeredVote.sender) {
      throw new ProtocolError("Undefined 'sender' parameter encountered during 'RegisteredVote'");
    }
    this.sender = registeredVote.sender;

    if (!registeredVote.votes) {
      throw new ProtocolError("Undefined 'votes' parameter encountered during 'RegisteredVote'");
    }
    this.votes = registeredVote.votes;

    if (!registeredVote.createdAt) {
      throw new ProtocolError(
        "Undefined 'createdAt' parameter encountered during 'RegisteredVote'",
      );
    }
    this.createdAt = registeredVote.createdAt;
  }

  public toState(): RegisteredVoteState {
    return {
      messageId: this.messageId.toState(),
      sender: this.sender.toState(),
      votes: this.votes.map((v) => v.toState()) as VoteState[] | EncryptedVoteState[],
      createdAt: this.createdAt.toState(),
    };
  }

  public static fromState(registeredVoteState: RegisteredVoteState) {
    let votes: Vote[] | EncryptedVote[] = [];

    if (registeredVoteState.votes.length >= 0) {
      if (typeof registeredVoteState.votes[0].vote === 'number') {
        // unencrypted votes directly contain the question index
        votes = (registeredVoteState.votes as VoteState[]).map(Vote.fromState);
      } else {
        // encrypted votes contain the question index in an encrypted form (i.e as a string)
        votes = (registeredVoteState.votes as EncryptedVoteState[]).map(EncryptedVote.fromState);
      }
    }

    return new RegisteredVote({
      messageId: Hash.fromState(registeredVoteState.messageId),
      sender: PublicKey.fromState(registeredVoteState.sender),
      votes,
      createdAt: Timestamp.fromState(registeredVoteState.createdAt),
    });
  }
}
