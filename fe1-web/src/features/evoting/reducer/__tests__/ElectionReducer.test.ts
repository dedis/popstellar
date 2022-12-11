import { AnyAction } from 'redux';

import { mockElectionNotStarted, mockElectionOpened } from 'features/evoting/__tests__/utils';
import { Election, ElectionState } from 'features/evoting/objects';

import {
  addElection,
  electionReduce,
  ElectionReducerState,
  ELECTION_REDUCER_PATH,
  makeElectionSelector,
  removeElection,
  updateElection,
} from '../ElectionReducer';

const mockElection = mockElectionNotStarted;
const mockElectionState: ElectionState = mockElection.toState();

const mockElection2 = mockElectionOpened;
const mockElectionState2: ElectionState = mockElection2.toState();

describe('ElectionReducer', () => {
  it('returns a valid initial state', () => {
    expect(electionReduce(undefined, {} as AnyAction)).toEqual({
      byId: {},
      allIds: [],
    } as ElectionReducerState);
  });

  describe('addElection', () => {
    it('adds new elections to the state', () => {
      expect(
        electionReduce(
          {
            byId: {},
            allIds: [],
          } as ElectionReducerState,
          addElection(mockElectionState),
        ),
      ).toEqual({
        byId: {
          [mockElectionState.id]: mockElectionState,
        },
        allIds: [mockElectionState.id],
      } as ElectionReducerState);
    });

    it('throws an error if the store already contains an election with the same id', () => {
      expect(() =>
        electionReduce(
          {
            byId: {
              [mockElectionState.id]: mockElectionState,
            },
            allIds: [mockElectionState.id],
          } as ElectionReducerState,
          addElection(mockElectionState),
        ),
      ).toThrow();
    });
  });

  describe('updateElection', () => {
    it('updates elections in the state', () => {
      expect(
        electionReduce(
          {
            byId: {
              [mockElectionState.id]: mockElectionState,
            },
            allIds: [mockElectionState.id],
          } as ElectionReducerState,
          updateElection(mockElectionState2),
        ),
      ).toEqual({
        byId: {
          [mockElectionState.id]: mockElectionState2,
        },
        allIds: [mockElectionState.id],
      } as ElectionReducerState);
    });

    it('throws an error when trying to update an inexistent election', () => {
      expect(() =>
        electionReduce(
          {
            byId: {},
            allIds: [],
          } as ElectionReducerState,
          updateElection(mockElectionState),
        ),
      ).toThrow();
    });
  });

  describe('removeElection', () => {
    it('removes elections from the state', () => {
      expect(
        electionReduce(
          {
            byId: {
              [mockElectionState.id]: mockElectionState,
            },
            allIds: [mockElectionState.id],
          } as ElectionReducerState,
          removeElection(mockElection.id),
        ),
      ).toEqual({
        byId: {},
        allIds: [],
      } as ElectionReducerState);
    });

    it('throws an error when trying to remove an inexistent election', () => {
      expect(() =>
        electionReduce(
          {
            byId: {},
            allIds: [],
          } as ElectionReducerState,
          removeElection(mockElection.id),
        ),
      ).toThrow();
    });
  });

  describe('makeElectionSelector', () => {
    it('returns the constructed election', () => {
      const election = makeElectionSelector(mockElection.id)({
        [ELECTION_REDUCER_PATH]: {
          byId: { [mockElectionState.id]: mockElectionState },
          allIds: [mockElectionState.id],
        } as ElectionReducerState,
      });
      expect(election).toBeInstanceOf(Election);
      expect(election?.toState()).toEqual(mockElectionState);
    });

    it('returns undefined if the id of the election is not in the store', () => {
      const election = makeElectionSelector(mockElection.id)({
        [ELECTION_REDUCER_PATH]: {
          byId: {},
          allIds: [],
        } as ElectionReducerState,
      });
      expect(election).toBeUndefined();
    });
  });
});
