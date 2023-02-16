import 'jest-extended';
import '__tests__/utils/matchers';

import { Hash } from 'core/objects';
import { OmitMethods } from 'core/types';

import { MajorityResult, QuestionResult } from '../index';

const mockQuestionId = new Hash('questionId');

const questionResult = new QuestionResult({
  id: mockQuestionId,
  result: [{ ballotOption: 'option1', count: 10 }],
});

describe('QuestionResult', () => {
  it('does a state round trip correctly', () => {
    const e = QuestionResult.fromState(questionResult.toState());
    expect(e).toEqual(questionResult);
  });

  describe('constructor', () => {
    it('throws an error when object is undefined', () => {
      const partial = undefined as unknown as OmitMethods<QuestionResult>;
      const createWrongQuestionResult = () => new QuestionResult(partial);
      expect(createWrongQuestionResult).toThrow(Error);
    });

    it("throws an error when 'id' is undefined", () => {
      const createWrongRegisteredVote = () =>
        new QuestionResult({
          id: undefined as unknown as Hash,
          result: [{ ballotOption: 'option1', count: 10 }],
        });
      expect(createWrongRegisteredVote).toThrow(Error);
    });

    it("throws an error when 'result' is undefined", () => {
      const createWrongRegisteredVote = () =>
        new QuestionResult({
          id: mockQuestionId,
          result: undefined as unknown as MajorityResult[],
        });
      expect(createWrongRegisteredVote).toThrow(Error);
    });
  });
});
