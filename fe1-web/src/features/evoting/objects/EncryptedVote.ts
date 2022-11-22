import { Hash, HashState } from 'core/objects';
import { OmitMethods } from 'core/types';

export interface EncryptedVoteState {
  id: HashState;
  question: HashState;
  vote: string;
}

export class EncryptedVote {
  public readonly id: Hash;

  public readonly question: Hash;

  public readonly vote: string;

  constructor(vote: OmitMethods<EncryptedVote>) {
    this.id = vote.id;
    this.question = vote.question;
    this.vote = vote.vote;
  }

  public toState(): EncryptedVoteState {
    return {
      id: this.id.toState(),
      question: this.question.toState(),
      vote: this.vote,
    };
  }

  public static fromState(encryptedVoteState: EncryptedVoteState): EncryptedVote {
    return new EncryptedVote({
      id: Hash.fromState(encryptedVoteState.id),
      question: Hash.fromState(encryptedVoteState.question),
      vote: encryptedVoteState.vote,
    });
  }
}
