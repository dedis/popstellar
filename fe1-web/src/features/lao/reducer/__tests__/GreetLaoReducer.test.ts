import { AnyAction } from 'redux';

import { mockLaoId } from '__tests__/utils';

import {
  addGreetLaoMessage,
  getAllGreetLaoMessageIds,
  greetLaoReduce,
  GreetLaoReducerState,
  GREET_LAO_REDUCER_PATH,
} from '../GreetLaoReducer';

const emptyState = {
  byLaoId: {},
} as GreetLaoReducerState;

const mockMessageId1 = 'someMessageId';
const mockMessageId2 = 'someOtherMessageId';

describe('GreetLaoReducer', () => {
  it('should return the initial state', () => {
    expect(greetLaoReduce(undefined, {} as AnyAction)).toEqual(emptyState);
  });

  describe('addGreetLaoMessage', () => {
    it('should add a lao#greet message id to the store', () => {
      expect(
        greetLaoReduce(
          {
            byLaoId: {
              [mockLaoId]: [mockMessageId1],
            },
          } as GreetLaoReducerState,
          addGreetLaoMessage({ laoId: mockLaoId, messageId: mockMessageId2 }),
        ),
      ).toEqual({
        byLaoId: {
          [mockLaoId]: [mockMessageId1, mockMessageId2],
        },
      } as GreetLaoReducerState);
    });

    it('should not add a lao#greet message id twice to the store', () => {
      expect(
        greetLaoReduce(
          {
            byLaoId: {
              [mockLaoId]: [mockMessageId1],
            },
          } as GreetLaoReducerState,
          addGreetLaoMessage({ laoId: mockLaoId, messageId: mockMessageId1 }),
        ),
      ).toEqual({
        byLaoId: {
          [mockLaoId]: [mockMessageId1],
        },
      } as GreetLaoReducerState);
    });
  });

  describe('getGreetLaoMessageIdsByLao', () => {
    it('should return the correct value', () => {
      expect(
        getAllGreetLaoMessageIds({
          [GREET_LAO_REDUCER_PATH]: {
            byLaoId: {
              [mockLaoId]: [mockMessageId1],
              someOtherLao: [mockMessageId2],
            },
          } as GreetLaoReducerState,
        }),
      ).toEqual([mockMessageId1, mockMessageId2]);
    });
  });
});
