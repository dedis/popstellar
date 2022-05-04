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
              someElectionId: 'someOtherKey',
            },
          },
          addElectionKey({ electionId: mockElectionId.valueOf(), electionKey: mockPublicKey }),
        ),
      ).toEqual({
        byElectionId: {
          someElectionId: 'someOtherKey',
          [mockElectionId.valueOf()]: mockPublicKey,
        },
      } as ElectionKeyReducerState);
    });

    it('should throw an error when trying to add an election key if one is already present', () => {
      expect(() => {
        electionKeyReduce(
          {
            byElectionId: {
              [mockElectionId.valueOf()]: mockPublicKey,
            },
          } as ElectionKeyReducerState,
          addElectionKey({
            electionId: mockElectionId.valueOf(),
            electionKey: mockPublicKey,
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
              [mockElectionId.valueOf()]: mockPublicKey,
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
            [mockElectionId.valueOf()]: mockPublicKey,
            someOtherId: 'someOtherKey',
          },
        } as ElectionKeyReducerState,
      })?.valueOf(),
    ).toEqual(mockPublicKey);
  });

  it('returns the undefined if no key is found for a given election id', () => {
    expect(
      getElectionKeyByElectionId(mockElectionId.valueOf(), {
        [ELECTION_KEY_REDUCER_PATH]: {
          byElectionId: {
            someOtherId: 'someOtherKey',
          },
        } as ElectionKeyReducerState,
      }),
    ).toBeUndefined();
  });
});
