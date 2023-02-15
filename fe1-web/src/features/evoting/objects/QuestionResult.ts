import { Hash, HashState, ProtocolError } from 'core/objects';
import { OmitMethods } from 'core/types';

export interface MajorityResult {
  ballotOption: string;
  count: number;
}

export interface QuestionResultState {
  id: HashState;
  result: MajorityResult[];
}

export class QuestionResult {
  id: Hash;

  result: MajorityResult[];

  constructor(questionResult: OmitMethods<QuestionResult>) {
    if (!questionResult) {
      throw new Error(
        'Error encountered while creating a QuestionResult object: undefined/null parameters',
      );
    }

    if (!questionResult.id) {
      throw new ProtocolError(
        "Undefined 'messageId' parameter encountered during 'QuestionResult'",
      );
    }
    this.id = questionResult.id;

    if (!questionResult.result) {
      throw new ProtocolError(
        "Undefined 'messageId' parameter encountered during 'QuestionResult'",
      );
    }
    this.result = questionResult.result;
  }

  public toState(): QuestionResultState {
    return {
      id: this.id.toState(),
      result: this.result,
    };
  }

  public static fromState(questionResultState: QuestionResultState): QuestionResult {
    return new QuestionResult({
      id: Hash.fromState(questionResultState.id),
      result: questionResultState.result,
    });
  }
}
