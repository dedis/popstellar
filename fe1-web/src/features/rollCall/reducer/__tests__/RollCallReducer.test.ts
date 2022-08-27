import { AnyAction } from 'redux';

import { mockRollCall, mockRollCall2, mockRollCallWithAliasState } from '../../__tests__/utils';
import { RollCall, RollCallState } from '../../objects';
import {
  addRollCall,
  getRollCallById,
  makeRollCallAttendeesListSelector,
  makeRollCallByIdSelector,
  makeRollCallSelector,
  removeRollCall,
  ROLLCALL_REDUCER_PATH,
  rollcallReduce,
  RollCallReducerState,
  updateRollCall,
} from '../RollCallReducer';

const mockRollCallState: RollCallState = mockRollCall.toState();
const mockRollCallState2: RollCallState = mockRollCall2.toState();

describe('RollCallReducer', () => {
  it('returns a valid initial state', () => {
    expect(rollcallReduce(undefined, {} as AnyAction)).toEqual({
      byId: {},
      allIds: [],
      idAlias: {},
    } as RollCallReducerState);
  });

  describe('addRollCall', () => {
    it('adds new rollcalls to the state', () => {
      expect(
        rollcallReduce(
          {
            byId: {},
            allIds: [],
            idAlias: {},
          } as RollCallReducerState,
          addRollCall(mockRollCallState),
        ),
      ).toEqual({
        byId: {
          [mockRollCallState.id]: mockRollCallState,
        },
        allIds: [mockRollCallState.id],
        idAlias: {},
      } as RollCallReducerState);
    });

    it('throws an error if the store already contains a rollcall with the same id', () => {
      expect(() =>
        rollcallReduce(
          {
            byId: {
              [mockRollCallState.id]: mockRollCallState,
            },
            allIds: [mockRollCallState.id],
            idAlias: {},
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
            idAlias: {},
          } as RollCallReducerState,
          updateRollCall(mockRollCallState2),
        ),
      ).toEqual({
        byId: {
          [mockRollCallState.id]: mockRollCallState2,
        },
        allIds: [mockRollCallState.id],
        idAlias: {},
      } as RollCallReducerState);
    });

    it('throws an error when trying to update an inexistent rollcall', () => {
      expect(() =>
        rollcallReduce(
          {
            byId: {},
            allIds: [],
            idAlias: {},
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
              [mockRollCallWithAliasState.id]: mockRollCallWithAliasState,
            },
            allIds: [mockRollCallWithAliasState.id],
            idAlias: { [mockRollCallWithAliasState.idAlias]: mockRollCallWithAliasState.id },
          } as RollCallReducerState,
          removeRollCall(mockRollCallWithAliasState.id),
        ),
      ).toEqual({
        byId: {},
        allIds: [],
        idAlias: {},
      } as RollCallReducerState);
    });

    it('throws an error when trying to remove an inexistent rollcall', () => {
      expect(() =>
        rollcallReduce(
          {
            byId: {},
            allIds: [],
            idAlias: {},
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
          idAlias: {},
        } as RollCallReducerState,
      });
      expect(rollcall).toBeInstanceOf(RollCall);
      expect(rollcall?.toState()).toEqual(mockRollCallState);
    });

    it('returns the constructed rollcall given an alias id', () => {
      const rollcall = makeRollCallSelector(mockRollCallWithAliasState.idAlias)({
        [ROLLCALL_REDUCER_PATH]: {
          byId: { [mockRollCallWithAliasState.id]: mockRollCallWithAliasState },
          allIds: [mockRollCallWithAliasState.id],
          idAlias: {
            [mockRollCallWithAliasState.idAlias]: mockRollCallWithAliasState.id,
          },
        } as RollCallReducerState,
      });
      expect(rollcall).toBeInstanceOf(RollCall);
      expect(rollcall?.toState()).toEqual(mockRollCallWithAliasState);
    });

    it('returns undefined if the id of the rollcall is not in the store', () => {
      const rollcall = makeRollCallSelector(mockRollCallState.id)({
        [ROLLCALL_REDUCER_PATH]: {
          byId: {},
          allIds: [],
          idAlias: {},
        } as RollCallReducerState,
      });
      expect(rollcall).toBeUndefined();
    });

    it('throws an error if there is an alias id stored but no rollcall for the corresponding id', () => {
      expect(() =>
        makeRollCallSelector(mockRollCallWithAliasState.idAlias)({
          [ROLLCALL_REDUCER_PATH]: {
            byId: {},
            allIds: [],
            idAlias: {
              [mockRollCallWithAliasState.idAlias]: mockRollCallWithAliasState.id,
            },
          } as RollCallReducerState,
        }),
      ).toThrow();
    });
  });

  describe('makeRollCallByIdSelector', () => {
    it('returns the constructed rollcall', () => {
      const rollcallMap = makeRollCallByIdSelector([mockRollCallState.id, 'someId'])({
        [ROLLCALL_REDUCER_PATH]: {
          byId: { [mockRollCallState.id]: mockRollCallState },
          allIds: [mockRollCallState.id],
          idAlias: {},
        } as RollCallReducerState,
      });

      expect(rollcallMap).toHaveProperty(mockRollCallState.id);
      expect(rollcallMap).not.toHaveProperty('someId');

      expect(rollcallMap[mockRollCallState.id]).toBeInstanceOf(RollCall);
      expect(rollcallMap[mockRollCallState.id]?.toState()).toEqual(mockRollCallState);
    });
  });

  describe('getRollCallById', () => {
    it('returns the constructed rollcall', () => {
      const rollcall = getRollCallById(mockRollCallState.id, {
        [ROLLCALL_REDUCER_PATH]: {
          byId: { [mockRollCallState.id]: mockRollCallState },
          allIds: [mockRollCallState.id],
          idAlias: {},
        } as RollCallReducerState,
      });
      expect(rollcall).toBeInstanceOf(RollCall);
      expect(rollcall?.toState()).toEqual(mockRollCallState);
    });

    it('returns the constructed rollcall given an alias id', () => {
      const rollcall = getRollCallById(mockRollCallWithAliasState.idAlias, {
        [ROLLCALL_REDUCER_PATH]: {
          byId: { [mockRollCallWithAliasState.id]: mockRollCallWithAliasState },
          allIds: [mockRollCallWithAliasState.id],
          idAlias: {
            [mockRollCallWithAliasState.idAlias]: mockRollCallWithAliasState.id,
          },
        } as RollCallReducerState,
      });
      expect(rollcall).toBeInstanceOf(RollCall);
      expect(rollcall?.toState()).toEqual(mockRollCallWithAliasState);
    });

    it('returns undefined if the id of the rollcall is not in the store', () => {
      const rollcall = getRollCallById(mockRollCallState.id, {
        [ROLLCALL_REDUCER_PATH]: {
          byId: {},
          allIds: [],
          idAlias: {},
        } as RollCallReducerState,
      });
      expect(rollcall).toBeUndefined();
    });

    it('throws an error if there is an alias id stored but no rollcall for the corresponding id', () => {
      expect(() =>
        getRollCallById(mockRollCallWithAliasState.idAlias, {
          [ROLLCALL_REDUCER_PATH]: {
            byId: {},
            allIds: [],
            idAlias: {
              [mockRollCallWithAliasState.idAlias]: mockRollCallWithAliasState.id,
            },
          } as RollCallReducerState,
        }),
      ).toThrow();
    });
  });

  describe('makeRollCallAttendeesList', () => {
    it('should return the attendee list', () => {
      expect(
        makeRollCallAttendeesListSelector(mockRollCallState.id)({
          [ROLLCALL_REDUCER_PATH]: {
            byId: { [mockRollCallState.id]: mockRollCallState },
            allIds: [mockRollCallState.id],
            idAlias: {},
          } as RollCallReducerState,
        }),
      ).toEqual(mockRollCall.attendees);
    });

    it('should return an empty list if the attendee list is undefined', () => {
      expect(
        makeRollCallAttendeesListSelector(mockRollCallState.id)({
          [ROLLCALL_REDUCER_PATH]: {
            byId: {
              [mockRollCallState.id]: {
                ...mockRollCallState,
                attendees: undefined,
              },
            },
            allIds: [mockRollCallState.id],
            idAlias: {},
          } as RollCallReducerState,
        }),
      ).toEqual([]);
    });

    it('should return an empty list, given undefined', () => {
      expect(
        makeRollCallAttendeesListSelector(undefined)({
          [ROLLCALL_REDUCER_PATH]: {
            byId: { [mockRollCallState.id]: mockRollCallState },
            allIds: [mockRollCallState.id],
            idAlias: {},
          } as RollCallReducerState,
        }),
      ).toEqual([]);
    });

    it('should return an empty list, given an invalid id', () => {
      expect(
        makeRollCallAttendeesListSelector('someId')({
          [ROLLCALL_REDUCER_PATH]: {
            byId: { [mockRollCallState.id]: mockRollCallState },
            allIds: [mockRollCallState.id],
            idAlias: {},
          } as RollCallReducerState,
        }),
      ).toEqual([]);
    });
  });
});
