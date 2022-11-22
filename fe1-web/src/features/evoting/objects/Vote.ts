import { Hash, HashState } from 'core/objects';
import { OmitMethods } from 'core/types';

export interface VoteState {
  id: HashState;
  question: HashState;
  vote: number;
}

export class Vote {
  public readonly id: Hash;

  public readonly question: Hash;

  public readonly vote: number;

  constructor(vote: OmitMethods<Vote>) {
    this.id = vote.id;
    this.question = vote.question;
    this.vote = vote.vote;
  }

  public toState(): VoteState {
    return {
      id: this.id.toState(),
      question: this.question.toState(),
      vote: this.vote,
    };
  }

  public static fromState(voteState: VoteState): Vote {
    return new Vote({
      id: Hash.fromState(voteState.id),
      question: Hash.fromState(voteState.question),
      vote: voteState.vote,
    });
  }
}
