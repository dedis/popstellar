import 'jest-extended';
import '__tests__/utils/matchers';

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
              someElectionId: 'someOtherElectionKey',
            },
          },
          addElectionKey({
            electionId: mockElectionId.valueOf(),
            electionKey: 'someElectionKey',
          }),
        ),
      ).toEqual({
        byElectionId: {
          someElectionId: 'someOtherElectionKey',
          [mockElectionId.valueOf()]: 'someElectionKey',
        },
      } as ElectionKeyReducerState);
    });

    it('should throw an error when trying to add an election key if one is already present', () => {
      expect(() => {
        electionKeyReduce(
          {
            byElectionId: {
              [mockElectionId.valueOf()]: 'someElectionKey',
            },
          } as ElectionKeyReducerState,
          addElectionKey({
            electionId: mockElectionId.valueOf(),
            electionKey: 'someElectionKey',
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
              [mockElectionId.valueOf()]: 'someElectionKey',
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
            [mockElectionId.valueOf()]: 'someElectionKey',
            someOtherId: 'someOtherElectionKey',
          },
        } as ElectionKeyReducerState,
      }),
    ).toBeJsonEqual('someElectionKey');
  });

  it('returns the undefined if no key is found for a given election id', () => {
    expect(
      getElectionKeyByElectionId(mockElectionId.valueOf(), {
        [ELECTION_KEY_REDUCER_PATH]: {
          byElectionId: {
            someOtherId: 'someElectionKey',
          },
        } as ElectionKeyReducerState,
      }),
    ).toBeUndefined();
  });
});
