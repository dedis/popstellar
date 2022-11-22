import { Hash } from 'core/objects';
import { OmitMethods } from 'core/types';

export interface QuestionState {
  id: string;
  question: string;
  voting_method: string;
  ballot_options: string[];
  write_in: boolean;
}

export class Question {
  public readonly id: Hash;

  public readonly question: string;

  public readonly ballot_options: string[];

  public readonly voting_method: string;

  // cannot remove this here as the protocol still requires the property to be there
  public readonly write_in: boolean;

  constructor(question: OmitMethods<Question>) {
    this.id = question.id;
    this.question = question.question;
    this.ballot_options = question.ballot_options;
    this.voting_method = question.voting_method;
    this.write_in = question.write_in;
  }

  public toState(): QuestionState {
    return {
      id: this.id.toState(),
      question: this.question,
      ballot_options: this.ballot_options,
      voting_method: this.voting_method,
      write_in: this.write_in,
    };
  }

  public static fromState(questionState: QuestionState): Question {
    return new Question({
      id: Hash.fromState(questionState.id),
      question: questionState.question,
      ballot_options: questionState.ballot_options,
      voting_method: questionState.voting_method,
      write_in: questionState.write_in,
    });
  }
}
