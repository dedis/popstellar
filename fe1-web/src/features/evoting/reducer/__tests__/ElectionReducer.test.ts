import { AnyAction } from 'redux';

import { mockElectionNotStarted, mockElectionOpened } from 'features/evoting/__tests__/utils';
import { Election, ElectionState, EMPTY_QUESTION } from 'features/evoting/objects';

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
const defaultElectionReducerState: ElectionReducerState = {
  byId: {},
  allIds: [],
  defaultQuestions: [EMPTY_QUESTION],
};
describe('ElectionReducer', () => {
  it('returns a valid initial state', () => {
    expect(electionReduce(undefined, {} as AnyAction)).toEqual(defaultElectionReducerState);
  });
  describe('addElection', () => {
    it('adds new elections to the state', () => {
      expect(electionReduce(defaultElectionReducerState, addElection(mockElectionState))).toEqual({
        byId: {
          [mockElectionState.id]: mockElectionState,
        },
        allIds: [mockElectionState.id],
        defaultQuestions: [EMPTY_QUESTION],
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
            defaultQuestions: [EMPTY_QUESTION],
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
            defaultQuestions: [EMPTY_QUESTION],
          } as ElectionReducerState,
          updateElection(mockElectionState2),
        ),
      ).toEqual({
        byId: {
          [mockElectionState.id]: mockElectionState2,
        },
        allIds: [mockElectionState.id],
        defaultQuestions: [EMPTY_QUESTION],
      } as ElectionReducerState);
    });

    it('throws an error when trying to update an inexistent election', () => {
      expect(() =>
        electionReduce(defaultElectionReducerState, updateElection(mockElection.id)),
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
            defaultQuestions: [EMPTY_QUESTION],
          } as ElectionReducerState,
          removeElection(mockElection.id),
        ),
      ).toEqual(defaultElectionReducerState);
    });

    it('throws an error when trying to remove an inexistent election', () => {
      expect(() =>
        electionReduce(defaultElectionReducerState, removeElection(mockElection.id)),
      ).toThrow();
    });
  });

  describe('makeElectionSelector', () => {
    it('returns the constructed election', () => {
      const election = makeElectionSelector(mockElection.id)({
        [ELECTION_REDUCER_PATH]: {
          byId: { [mockElectionState.id]: mockElectionState },
          allIds: [mockElectionState.id],
          defaultQuestions: [EMPTY_QUESTION],
        } as ElectionReducerState,
      });
      expect(election).toBeInstanceOf(Election);
      expect(election?.toState()).toEqual(mockElectionState);
    });

    it('returns undefined if the id of the election is not in the store', () => {
      const election = makeElectionSelector(mockElection.id)({
        [ELECTION_REDUCER_PATH]: defaultElectionReducerState,
      });
      expect(election).toBeUndefined();
    });
  });
});
