import 'jest-extended';
import '__tests__/utils/matchers';

import {
  mockElectionId,
  mockElectionKey,
  mockElectionKeyString,
} from 'features/evoting/__tests__/utils';

import {
  addElectionKey,
  electionKeyReduce,
  ElectionKeyReducerState,
  ELECTION_KEY_REDUCER_PATH,
  getElectionKeyByElectionId,
  makeElectionKeySelector,
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
            electionKey: mockElectionKeyString,
          }),
        ),
      ).toEqual({
        byElectionId: {
          someElectionId: 'someOtherElectionKey',
          [mockElectionId.valueOf()]: mockElectionKeyString,
        },
      } as ElectionKeyReducerState);
    });

    it('should throw an error when trying to add an election key if one is already present', () => {
      expect(() => {
        electionKeyReduce(
          {
            byElectionId: {
              [mockElectionId.valueOf()]: mockElectionKeyString,
            },
          } as ElectionKeyReducerState,
          addElectionKey({
            electionId: mockElectionId.valueOf(),
            electionKey: mockElectionKeyString,
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
              [mockElectionId.valueOf()]: mockElectionKeyString,
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
            [mockElectionId.valueOf()]: mockElectionKeyString,
            someOtherId: 'someOtherElectionKey',
          },
        } as ElectionKeyReducerState,
      }),
    ).toEqual(mockElectionKey);
  });

  it('returns the undefined if no key is found for a given election id', () => {
    expect(
      getElectionKeyByElectionId(mockElectionId.valueOf(), {
        [ELECTION_KEY_REDUCER_PATH]: {
          byElectionId: {
            someOtherId: mockElectionKeyString,
          },
        } as ElectionKeyReducerState,
      }),
    ).toBeUndefined();
  });
});

describe('makeElectionKeySelector', () => {
  it('returns the correct key for a given election id', () => {
    expect(
      makeElectionKeySelector(mockElectionId.valueOf())({
        [ELECTION_KEY_REDUCER_PATH]: {
          byElectionId: {
            [mockElectionId.valueOf()]: mockElectionKeyString,
            someOtherId: 'someOtherElectionKey',
          },
        } as ElectionKeyReducerState,
      }),
    ).toEqual(mockElectionKey);
  });

  it('returns the undefined if no key is found for a given election id', () => {
    expect(
      makeElectionKeySelector(mockElectionId.valueOf())({
        [ELECTION_KEY_REDUCER_PATH]: {
          byElectionId: {
            someOtherId: mockElectionKeyString,
          },
        } as ElectionKeyReducerState,
      }),
    ).toBeUndefined();
  });
});
