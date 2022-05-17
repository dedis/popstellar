import 'jest-extended';
import '__tests__/utils/matchers';
import { describe } from '@jest/globals';

import { configureTestFeatures } from '__tests__/utils';

import {
  addMessageToWitness,
  isMessageToWitness,
  MessagesToWitnessReducerState,
  removeMessageToWitness,
  witnessReduce,
  WITNESS_REDUCER_PATH,
} from '../WitnessReducer';

const mockMessageId1 = 'someMessageId';

beforeAll(() => {
  // we need to set up the message registry for Message.fromData to work
  configureTestFeatures();
});

describe('WitnessReducer', () => {
  describe('addMessageToWitness', () => {
    it('adds messages to the store', () => {
      const newState = witnessReduce(
        { allIds: [] },
        addMessageToWitness({ messageId: mockMessageId1 }),
      );

      expect(newState.allIds).toEqual([mockMessageId1]);
    });

    it("doesn't add a message a second time to the store", () => {
      const newState = witnessReduce(
        witnessReduce({ allIds: [] }, addMessageToWitness({ messageId: mockMessageId1 })),
        addMessageToWitness({ messageId: mockMessageId1 }),
      );

      expect(newState.allIds).toEqual([mockMessageId1]);
    });
  });

  describe('removeMessageToWitness', () => {
    it('removes the witnesses message from the store', () => {
      const newState = witnessReduce(
        {
          allIds: [mockMessageId1],
        } as MessagesToWitnessReducerState,
        removeMessageToWitness(mockMessageId1),
      );

      expect(newState.allIds).toEqual([]);
    });

    it("doesn't do anything of the id is not in the store", () => {
      const newState = witnessReduce(
        {
          allIds: [mockMessageId1],
        } as MessagesToWitnessReducerState,
        removeMessageToWitness('some other id'),
      );

      expect(newState.allIds).toEqual([mockMessageId1]);
    });
  });

  describe('getMessageToWitness', () => {
    it('returns the correct data', () => {
      expect(
        isMessageToWitness(mockMessageId1, {
          [WITNESS_REDUCER_PATH]: {
            allIds: [mockMessageId1],
          } as MessagesToWitnessReducerState,
        }),
      ).toBeTrue();

      expect(
        isMessageToWitness(mockMessageId1, {
          [WITNESS_REDUCER_PATH]: {
            allIds: [],
          } as MessagesToWitnessReducerState,
        }),
      ).toBeFalse();
    });
  });
});
