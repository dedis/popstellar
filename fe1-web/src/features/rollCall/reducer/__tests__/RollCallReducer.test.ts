import { AnyAction } from 'redux';

import { mockRollCall, mockRollCall2 } from '../../__tests__/utils';
import { RollCall, RollCallState } from '../../objects';
import {
  addRollCall,
  rollcallReduce,
  RollCallReducerState,
  ROLLCALL_REDUCER_PATH,
  makeRollCallSelector,
  removeRollCall,
  updateRollCall,
  makeRollCallAttendeesListSelector,
} from '../RollCallReducer';

const mockRollCallState: RollCallState = mockRollCall.toState();
const mockRollCallState2: RollCallState = mockRollCall2.toState();

describe('RollCallReducer', () => {
  it('returns a valid initial state', () => {
    expect(rollcallReduce(undefined, {} as AnyAction)).toEqual({
      byId: {},
      allIds: [],
    } as RollCallReducerState);
  });

  describe('addRollCall', () => {
    it('adds new rollcalls to the state', () => {
      expect(
        rollcallReduce(
          {
            byId: {},
            allIds: [],
          } as RollCallReducerState,
          addRollCall(mockRollCallState),
        ),
      ).toEqual({
        byId: {
          [mockRollCallState.id]: mockRollCallState,
        },
        allIds: [mockRollCallState.id],
      } as RollCallReducerState);
    });

    it('throws an error if the store already contains an rollcall with the same id', () => {
      expect(() =>
        rollcallReduce(
          {
            byId: {
              [mockRollCallState.id]: mockRollCallState,
            },
            allIds: [mockRollCallState.id],
          } as RollCallReducerState,
          addRollCall(mockRollCallState),
        ),
      ).toThrow();
    });
  });

  describe('updateRollCall', () => {
    it('updates rollcalls in the state', () => {
      expect(
        rollcallReduce(
          {
            byId: {
              [mockRollCallState.id]: mockRollCallState,
            },
            allIds: [mockRollCallState.id],
          } as RollCallReducerState,
          updateRollCall(mockRollCallState2),
        ),
      ).toEqual({
        byId: {
          [mockRollCallState.id]: mockRollCallState2,
        },
        allIds: [mockRollCallState.id],
      } as RollCallReducerState);
    });

    it('throws an error when trying to update an inexistent rollcall', () => {
      expect(() =>
        rollcallReduce(
          {
            byId: {},
            allIds: [],
          } as RollCallReducerState,
          updateRollCall(mockRollCallState),
        ),
      ).toThrow();
    });
  });

  describe('removeRollCall', () => {
    it('removes rollcalls from the state', () => {
      expect(
        rollcallReduce(
          {
            byId: {
              [mockRollCallState.id]: mockRollCallState,
            },
            allIds: [mockRollCallState.id],
          } as RollCallReducerState,
          removeRollCall(mockRollCallState.id),
        ),
      ).toEqual({
        byId: {},
        allIds: [],
      } as RollCallReducerState);
    });

    it('throws an error when trying to remove an inexistent rollcall', () => {
      expect(() =>
        rollcallReduce(
          {
            byId: {},
            allIds: [],
          } as RollCallReducerState,
          removeRollCall(mockRollCallState.id),
        ),
      ).toThrow();
    });
  });

  describe('makeRollCallSelector', () => {
    it('returns the constructed rollcall', () => {
      const rollcall = makeRollCallSelector(mockRollCallState.id)({
        [ROLLCALL_REDUCER_PATH]: {
          byId: { [mockRollCallState.id]: mockRollCallState },
          allIds: [mockRollCallState.id],
        } as RollCallReducerState,
      });
      expect(rollcall).toBeInstanceOf(RollCall);
      expect(rollcall?.toState()).toEqual(mockRollCallState);
    });

    it('returns undefined if the id of the rollcall is not in the store', () => {
      const rollcall = makeRollCallSelector(mockRollCallState.id)({
        [ROLLCALL_REDUCER_PATH]: {
          byId: {},
          allIds: [],
        } as RollCallReducerState,
      });
      expect(rollcall).toBeUndefined();
    });
  });

  describe('makeRollCallAttendeesList', () => {
    it('should return the attendee list', () => {
      expect(
        makeRollCallAttendeesListSelector(mockRollCallState.id)({
          [ROLLCALL_REDUCER_PATH]: {
            byId: { [mockRollCallState.id]: mockRollCallState },
            allIds: [mockRollCallState.id],
          } as RollCallReducerState,
        }),
      ).toEqual(mockRollCall.attendees);
    });
  });
});
