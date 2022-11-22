import 'jest-extended';
import '__tests__/utils/matchers';
import { describe } from '@jest/globals';

import { configureTestFeatures } from '__tests__/utils';
import { Hash } from 'core/objects';

import {
  addMessageToWitness,
  isMessageToWitness,
  MessagesToWitnessReducerState,
  removeMessageToWitness,
  witnessReduce,
  WITNESS_REDUCER_PATH,
} from '../WitnessReducer';

const mockMessageId1 = new Hash('someMessageId');

beforeAll(() => {
  // we need to set up the message registry for Message.fromData to work
  configureTestFeatures();
});

describe('WitnessReducer', () => {
  describe('addMessageToWitness', () => {
    it('adds messages to the store', () => {
      const newState = witnessReduce({ allIds: [] }, addMessageToWitness(mockMessageId1));

      expect(newState.allIds).toEqual([mockMessageId1]);
    });

    it("doesn't add a message a second time to the store", () => {
      const newState = witnessReduce(
        witnessReduce({ allIds: [] }, addMessageToWitness(mockMessageId1)),
        addMessageToWitness(mockMessageId1),
      );

      expect(newState.allIds).toEqual([mockMessageId1]);
    });
  });

  describe('removeMessageToWitness', () => {
    it('removes the witnesses message from the store', () => {
      const newState = witnessReduce(
        {
          allIds: [mockMessageId1.serialize()],
        } as MessagesToWitnessReducerState,
        removeMessageToWitness(mockMessageId1),
      );

      expect(newState.allIds).toEqual([]);
    });

    it("doesn't do anything of the id is not in the store", () => {
      const newState = witnessReduce(
        {
          allIds: [mockMessageId1.serialize()],
        } as MessagesToWitnessReducerState,
        removeMessageToWitness(new Hash('some other id')),
      );

      expect(newState.allIds).toEqual([mockMessageId1]);
    });
  });

  describe('getMessageToWitness', () => {
    it('returns the correct data', () => {
      expect(
        isMessageToWitness(mockMessageId1, {
          [WITNESS_REDUCER_PATH]: {
            allIds: [mockMessageId1.serialize()],
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
