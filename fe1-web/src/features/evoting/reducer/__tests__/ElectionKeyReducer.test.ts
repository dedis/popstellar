import 'jest-extended';
import '__tests__/utils/matchers';

import { mockElectionId } from 'features/evoting/__tests__/utils';

import {
  addElectionKeyMessage,
  electionKeyReduce,
  ElectionKeyMessageReducerState,
  ELECTION_KEY_MESSAGE_REDUCER_PATH,
  getElectionKeyMessageIdByElectionId,
  removeElectionKeyMessage,
} from '../ElectionKeyReducer';

describe('ElectionKeyReducer', () => {
  describe('addElectionKey', () => {
    it('should add an election key to the state if its not already present', () => {
      expect(
        electionKeyReduce(
          {
            byElectionId: {
              someElectionId: 'someOtherMessageId',
            },
          },
          addElectionKeyMessage({
            electionId: mockElectionId.valueOf(),
            messageId: 'someMessageId',
          }),
        ),
      ).toEqual({
        byElectionId: {
          someElectionId: 'someOtherMessageId',
          [mockElectionId.valueOf()]: 'someMessageId',
        },
      } as ElectionKeyMessageReducerState);
    });

    it('should throw an error when trying to add an election key if one is already present', () => {
      expect(() => {
        electionKeyReduce(
          {
            byElectionId: {
              [mockElectionId.valueOf()]: 'someMessageId',
            },
          } as ElectionKeyMessageReducerState,
          addElectionKeyMessage({
            electionId: mockElectionId.valueOf(),
            messageId: 'someMessageId',
          }),
        );
      }).toThrow();
    });
  });

  describe('removeElectionKey', () => {
    it('should remove a stored election key for a given election id', () => {
      expect(
        electionKeyReduce(
          {
            byElectionId: {
              [mockElectionId.valueOf()]: 'someMessageId',
            },
          } as ElectionKeyMessageReducerState,
          removeElectionKeyMessage(mockElectionId.valueOf()),
        ),
      ).toEqual({
        byElectionId: {},
      } as ElectionKeyMessageReducerState);
    });

    it('should not throw an error if no key is stored for a given election id', () => {
      expect(() =>
        electionKeyReduce(
          {
            byElectionId: {},
          } as ElectionKeyMessageReducerState,
          removeElectionKeyMessage(mockElectionId.valueOf()),
        ),
      ).not.toThrow();
    });
  });
});

describe('getElectionKeyByElectionId', () => {
  it('returns the correct key for a given election id', () => {
    expect(
      getElectionKeyMessageIdByElectionId(mockElectionId.valueOf(), {
        [ELECTION_KEY_MESSAGE_REDUCER_PATH]: {
          byElectionId: {
            [mockElectionId.valueOf()]: 'someMessageId',
            someOtherId: 'someOtherMessageId',
          },
        } as ElectionKeyMessageReducerState,
      }),
    ).toBeJsonEqual('someMessageId');
  });

  it('returns the undefined if no key is found for a given election id', () => {
    expect(
      getElectionKeyMessageIdByElectionId(mockElectionId.valueOf(), {
        [ELECTION_KEY_MESSAGE_REDUCER_PATH]: {
          byElectionId: {
            someOtherId: 'someMessageId',
          },
        } as ElectionKeyMessageReducerState,
      }),
    ).toBeUndefined();
  });
});
