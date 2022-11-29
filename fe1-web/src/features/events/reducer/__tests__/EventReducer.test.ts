import 'jest-extended';

import { AnyAction } from 'redux';

import { mockLaoId } from '__tests__/utils/TestUtils';
import { mockElectionNotStarted } from 'features/evoting/__tests__/utils';
import { Election } from 'features/evoting/objects';
import {
  mockRollCallState,
  mockRollCallState2,
  mockRollCallWithAlias,
  mockRollCallWithAliasState,
} from 'features/rollCall/__tests__/utils';
import { RollCall } from 'features/rollCall/objects';

import {
  addEvent,
  clearAllEvents,
  eventReduce,
  EventReducerState,
  EVENT_REDUCER_PATH,
  makeEventByTypeSelector,
  makeEventListSelector,
  makeEventSelector,
  removeEvent,
  updateEvent,
} from '../EventReducer';

describe('EventReducer', () => {
  it('should return the initial state', () => {
    expect(eventReduce(undefined, {} as AnyAction)).toEqual({
      byLaoId: {},
      byId: {},
    } as EventReducerState);
  });

  it('should add the created roll call', () => {
    expect(
      eventReduce(
        {
          byLaoId: {
            [mockLaoId]: {
              allIds: [],
            },
          },
          byId: {},
        } as EventReducerState,
        addEvent(mockLaoId, {
          eventType: RollCall.EVENT_TYPE,
          id: mockRollCallWithAliasState.id,
          start: mockRollCallWithAlias.start.valueOf(),
          end: mockRollCallWithAlias.end?.valueOf(),
        }),
      ),
    ).toEqual({
      byLaoId: {
        [mockLaoId]: {
          allIds: [mockRollCallWithAliasState.id],
        },
      },
      byId: {
        [mockRollCallWithAliasState.id]: {
          eventType: RollCall.EVENT_TYPE,
          id: mockRollCallWithAliasState.id,
          start: mockRollCallWithAlias.start.valueOf(),
          end: mockRollCallWithAlias.end?.valueOf(),
        },
      },
    } as EventReducerState);
  });

  it('should update the created roll call', () => {
    expect(
      eventReduce(
        {
          byLaoId: {
            [mockLaoId]: {
              allIds: [mockRollCallState.id],
            },
          },
          byId: {
            [mockRollCallState.id]: {
              eventType: RollCall.EVENT_TYPE,
              id: mockRollCallState.id,
            },
          },
        } as EventReducerState,
        updateEvent({
          eventType: RollCall.EVENT_TYPE,
          id: mockRollCallWithAliasState.id,
          start: mockRollCallWithAlias.start.valueOf(),
          end: mockRollCallWithAlias.end?.valueOf(),
        }),
      ),
    ).toEqual({
      byLaoId: {
        [mockLaoId]: {
          allIds: [mockRollCallState.id],
        },
      },
      byId: {
        [mockRollCallState.id]: {
          eventType: RollCall.EVENT_TYPE,
          id: mockRollCallState.id,
          start: mockRollCallWithAlias.start.valueOf(),
          end: mockRollCallWithAlias.end?.valueOf(),
        },
      },
    } as EventReducerState);
  });

  it('should remove the roll call', () => {
    expect(
      eventReduce(
        {
          byLaoId: {
            [mockLaoId]: {
              allIds: [mockRollCallState.id],
            },
          },
          byId: {
            [mockRollCallState.id]: {
              eventType: RollCall.EVENT_TYPE,
              id: mockRollCallState.id,
            },
          },
        } as EventReducerState,
        removeEvent(mockLaoId, mockRollCallState.id),
      ),
    ).toEqual({
      byLaoId: {
        [mockLaoId]: {
          allIds: [],
        },
      },
      byId: {},
    } as EventReducerState);
  });

  it('should clear all events', () => {
    expect(
      eventReduce(
        {
          byLaoId: {
            [mockLaoId]: {
              allIds: [mockRollCallState.id, mockRollCallState2.id],
            },
          },
          byId: {
            [mockRollCallState.id]: {
              eventType: RollCall.EVENT_TYPE,
              id: mockRollCallState.id,
            },
            [mockRollCallState2.id]: {
              eventType: RollCall.EVENT_TYPE,
              id: mockRollCallState2.id,
            },
          },
        } as EventReducerState,
        clearAllEvents(),
      ),
    ).toEqual({
      byLaoId: {},
      byId: {},
    } as EventReducerState);
  });
});

const filledState = {
  [EVENT_REDUCER_PATH]: {
    byLaoId: {
      [mockLaoId]: {
        allIds: [mockRollCallState.id],
      },
      someOtherId: {
        allIds: ['otherId', mockElectionNotStarted.id.valueOf()],
      },
    },
    byId: {
      [mockRollCallWithAliasState.id]: {
        eventType: RollCall.EVENT_TYPE,
        id: mockRollCallWithAliasState.id,
        start: mockRollCallWithAlias.start.valueOf(),
        end: mockRollCallWithAlias.end?.valueOf(),
      },
      otherId: {
        eventType: RollCall.EVENT_TYPE,
        id: 'otherId',
        start: 0,
      },
      [mockElectionNotStarted.id.valueOf()]: {
        eventType: Election.EVENT_TYPE,
        id: mockElectionNotStarted.id.valueOf(),
      },
    },
  } as EventReducerState,
};

describe('makeEventListSelector', () => {
  it('should return the correct value', () => {
    expect(makeEventListSelector(mockLaoId)(filledState)).toEqual([
      {
        eventType: RollCall.EVENT_TYPE,
        id: mockRollCallState.id,
        start: mockRollCallWithAlias.start.valueOf(),
        end: mockRollCallWithAlias.end?.valueOf(),
      },
    ]);
  });

  it('should return an empty list if the given laoId is not stored', () => {
    expect(
      makeEventListSelector(mockLaoId)({
        [EVENT_REDUCER_PATH]: { byLaoId: {} } as EventReducerState,
      }),
    ).toEqual([]);
  });
});

describe('makeEventSelector', () => {
  it('should return the correct value', () => {
    expect(makeEventSelector(mockRollCallState.id)(filledState)).toEqual({
      eventType: RollCall.EVENT_TYPE,
      id: mockRollCallState.id,
      start: mockRollCallWithAlias.start.valueOf(),
      end: mockRollCallWithAlias.end?.valueOf(),
    });
  });

  it('should throw an error if the given laoId is not stored', () => {
    expect(() =>
      makeEventSelector(mockRollCallState.id)({ byLaoId: {} } as EventReducerState),
    ).toThrow();
  });

  it('should return undefined if the lao does not have an event with the provided id', () => {
    expect(makeEventSelector('some id')(filledState)).toBeUndefined();
  });
});

describe('makeEventByTypeSelector', () => {
  it('should return the correct value', () => {
    expect(makeEventByTypeSelector(mockLaoId, RollCall.EVENT_TYPE)(filledState)).toEqual({
      [mockRollCallWithAliasState.id]: {
        eventType: RollCall.EVENT_TYPE,
        id: mockRollCallWithAliasState.id,
        start: mockRollCallWithAlias.start.valueOf(),
        end: mockRollCallWithAlias.end?.valueOf(),
      },
    });
  });
});
