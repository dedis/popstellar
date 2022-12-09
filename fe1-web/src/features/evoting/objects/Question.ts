import { Hash, HashState, ProtocolError } from 'core/objects';
import { OmitMethods } from 'core/types';

export interface QuestionState {
  id: HashState;
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
    if (typeof question.id === 'undefined') {
      throw new ProtocolError(
        "Undefined 'id' parameter encountered during construction of 'Question'",
      );
    }
    this.id = question.id;

    if (typeof question.question === 'undefined') {
      throw new ProtocolError(
        "Undefined 'question' parameter encountered during construction of 'Question'",
      );
    }
    this.question = question.question;

    if (typeof question.ballot_options === 'undefined') {
      throw new ProtocolError(
        "Undefined 'ballot_options' parameter encountered during construction of 'Question'",
      );
    }
    this.ballot_options = question.ballot_options;

    if (typeof question.voting_method === 'undefined') {
      throw new ProtocolError(
        "Undefined 'voting_method' parameter encountered during construction of 'Question'",
      );
    }
    this.voting_method = question.voting_method;

    if (typeof question.write_in === 'undefined') {
      throw new ProtocolError(
        "Undefined 'write_in' parameter encountered during construction of 'Question'",
      );
    }
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

  public static fromJson(questionJson: any): Question {
    return new Question({
      id: new Hash(questionJson.id),
      question: questionJson.question,
      ballot_options: questionJson.ballot_options,
      voting_method: questionJson.voting_method,
      write_in: questionJson.write_in,
    });
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
