import 'jest-extended';
import '__tests__/utils/matchers';

import { mockPublicKey } from '__tests__/utils';
import { mockElectionId } from 'features/evoting/__tests__/utils';

import {
  addElectionKey,
  electionKeyReduce,
  ElectionKeyReducerState,
  ELECTION_KEY_REDUCER_PATH,
  getElectionKeyByElectionId,
  removeElectionKey,
} from '../ElectionKeyReducer';

describe('ElectionKeyReducer', () => {
  describe('addElectionKey', () => {
    it('should add an election key to the state if its not already present', () => {
      expect(
        electionKeyReduce(
          {
            byElectionId: {
              someElectionId: {
                electionKey: 'someOtherKey',
                messageId: 'someOtherMessageId',
              },
            },
          },
          addElectionKey({
            electionId: mockElectionId.valueOf(),
            electionKey: mockPublicKey,
            messageId: 'someMessageId',
          }),
        ),
      ).toEqual({
        byElectionId: {
          someElectionId: {
            electionKey: 'someOtherKey',
            messageId: 'someOtherMessageId',
          },
          [mockElectionId.valueOf()]: {
            electionKey: mockPublicKey,
            messageId: 'someMessageId',
          },
        },
      } as ElectionKeyReducerState);
    });

    it('should throw an error when trying to add an election key if one is already present', () => {
      expect(() => {
        electionKeyReduce(
          {
            byElectionId: {
              [mockElectionId.valueOf()]: {
                electionKey: mockPublicKey,
                messageId: 'someMessageId',
              },
            },
          } as ElectionKeyReducerState,
          addElectionKey({
            electionId: mockElectionId.valueOf(),
            electionKey: mockPublicKey,
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
              [mockElectionId.valueOf()]: {
                electionKey: mockPublicKey,
                messageId: 'mockPublicKey',
              },
            },
          } as ElectionKeyReducerState,
          removeElectionKey(mockElectionId.valueOf()),
        ),
      ).toEqual({
        byElectionId: {},
      } as ElectionKeyReducerState);
    });

    it('should not throw an error if no key is stored for a given election id', () => {
      expect(() =>
        electionKeyReduce(
          {
            byElectionId: {},
          } as ElectionKeyReducerState,
          removeElectionKey(mockElectionId.valueOf()),
        ),
      ).not.toThrow();
    });
  });
});

describe('getElectionKeyByElectionId', () => {
  it('returns the correct key for a given election id', () => {
    expect(
      getElectionKeyByElectionId(mockElectionId.valueOf(), {
        [ELECTION_KEY_REDUCER_PATH]: {
          byElectionId: {
            [mockElectionId.valueOf()]: {
              electionKey: mockPublicKey,
              messageId: 'someMessageId',
            },
            someOtherId: {
              electionKey: 'someOtherKey',
              messageId: 'someOtherMessageId',
            },
          },
        } as ElectionKeyReducerState,
      }),
    ).toBeJsonEqual({ electionKey: mockPublicKey, messageId: 'someMessageId' });
  });

  it('returns the undefined if no key is found for a given election id', () => {
    expect(
      getElectionKeyByElectionId(mockElectionId.valueOf(), {
        [ELECTION_KEY_REDUCER_PATH]: {
          byElectionId: {
            someOtherId: {
              electionKey: 'someOtherKey',
              messageId: 'someMessageId',
            },
          },
        } as ElectionKeyReducerState,
      }),
    ).toBeUndefined();
  });
});
