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
  makeEventAliasMapSelector,
  makeEventByTypeSelector,
  makeEventListSelector,
  makeEventMapSelector,
  makeEventSelector,
  removeEvent,
  updateEvent,
} from '../EventReducer';

describe('EventReducer', () => {
  it('should return the initial state', () => {
    expect(eventReduce(undefined, {} as AnyAction)).toEqual({
      byLaoId: {},
    } as EventReducerState);
  });

  it('should add the created roll call', () => {
    expect(
      eventReduce(
        {
          byLaoId: {
            [mockLaoId]: {
              allIds: [],
              byId: {},
              idAlias: {},
            },
          },
        } as EventReducerState,
        addEvent(
          mockLaoId,
          RollCall.EVENT_TYPE,
          mockRollCallWithAlias.id,
          mockRollCallWithAlias.idAlias,
        ),
      ),
    ).toEqual({
      byLaoId: {
        [mockLaoId]: {
          allIds: [mockRollCallWithAliasState.id],
          byId: {
            [mockRollCallWithAliasState.id]: {
              eventType: RollCall.EVENT_TYPE,
              id: mockRollCallWithAliasState.id,
              idAlias: mockRollCallWithAliasState.idAlias,
            },
          },
          idAlias: {
            [mockRollCallWithAliasState.idAlias]: mockRollCallWithAliasState.id,
          },
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
              byId: {
                [mockRollCallState.id]: {
                  eventType: RollCall.EVENT_TYPE,
                  id: mockRollCallState.id,
                  idAlias: undefined,
                },
              },
              idAlias: {},
            },
          },
        } as EventReducerState,
        updateEvent(
          mockLaoId,
          RollCall.EVENT_TYPE,
          mockRollCallWithAliasState.id,
          mockRollCallWithAliasState.idAlias,
        ),
      ),
    ).toEqual({
      byLaoId: {
        [mockLaoId]: {
          allIds: [mockRollCallState.id],
          byId: {
            [mockRollCallState.id]: {
              eventType: RollCall.EVENT_TYPE,
              id: mockRollCallState.id,
              idAlias: mockRollCallWithAliasState.idAlias,
            },
          },
          idAlias: {
            [mockRollCallWithAliasState.idAlias]: mockRollCallState.id,
          },
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
              byId: {
                [mockRollCallState.id]: {
                  eventType: RollCall.EVENT_TYPE,
                  id: mockRollCallState.id,
                  idAlias: mockRollCallWithAliasState.idAlias,
                },
              },
              idAlias: {
                [mockRollCallWithAliasState.idAlias]: mockRollCallState.id,
              },
            },
          },
        } as EventReducerState,
        removeEvent(mockLaoId, mockRollCallState.id),
      ),
    ).toEqual({
      byLaoId: {
        [mockLaoId]: {
          allIds: [],
          byId: {},
          idAlias: {},
        },
      },
    } as EventReducerState);
  });

  it('should clear all events', () => {
    expect(
      eventReduce(
        {
          byLaoId: {
            [mockLaoId]: {
              allIds: [mockRollCallState.id, mockRollCallState2.id],
              byId: {
                [mockRollCallState.id]: {
                  eventType: RollCall.EVENT_TYPE,
                  id: mockRollCallState.id,
                  idAlias: mockRollCallWithAliasState.idAlias,
                },
                [mockRollCallState2.id]: {
                  eventType: RollCall.EVENT_TYPE,
                  id: mockRollCallState2.id,
                },
              },
              idAlias: {
                [mockRollCallWithAliasState.idAlias]: mockRollCallState.id,
              },
            },
          },
        } as EventReducerState,
        clearAllEvents(),
      ),
    ).toEqual({
      byLaoId: {},
    } as EventReducerState);
  });
});

const filledState = {
  [EVENT_REDUCER_PATH]: {
    byLaoId: {
      [mockLaoId]: {
        allIds: [mockRollCallState.id],
        byId: {
          [mockRollCallState.id]: {
            eventType: RollCall.EVENT_TYPE,
            id: mockRollCallState.id,
            idAlias: mockRollCallWithAliasState.idAlias,
          },
        },
        idAlias: {
          [mockRollCallWithAliasState.idAlias]: mockRollCallState.id,
        },
      },
      someOtherId: {
        allIds: [mockRollCallState2.id, mockElectionNotStarted.id.valueOf()],
        byId: {
          [mockRollCallState2.id]: {
            eventType: RollCall.EVENT_TYPE,
            id: mockRollCallState2.id,
          },
          [mockElectionNotStarted.id.valueOf()]: {
            eventType: Election.EVENT_TYPE,
            id: mockElectionNotStarted.id.valueOf(),
          },
        },
        idAlias: {},
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
        idAlias: mockRollCallWithAliasState.idAlias,
      },
    ]);
  });

  it('should throw an error if the given laoId is not stored', () => {
    expect(() => makeEventListSelector(mockLaoId)({ byLaoId: {} } as EventReducerState)).toThrow();
  });
});

describe('makeEventAliasMapSelector', () => {
  it('should return the correct value', () => {
    expect(makeEventAliasMapSelector(mockLaoId)(filledState)).toEqual({
      [mockRollCallWithAliasState.idAlias]: mockRollCallState.id,
    });
  });

  it('should throw an error if the given laoId is not stored', () => {
    expect(() =>
      makeEventAliasMapSelector(mockLaoId)({ byLaoId: {} } as EventReducerState),
    ).toThrow();
  });
});

describe('makeEventMapSelector', () => {
  it('should return the correct value', () => {
    expect(makeEventMapSelector(mockLaoId)(filledState)).toEqual({
      [mockRollCallState.id]: {
        eventType: RollCall.EVENT_TYPE,
        id: mockRollCallState.id,
        idAlias: mockRollCallWithAliasState.idAlias,
      },
    });
  });

  it('should throw an error if the given laoId is not stored', () => {
    expect(() => makeEventMapSelector(mockLaoId)({ byLaoId: {} } as EventReducerState)).toThrow();
  });
});

describe('makeEventSelector', () => {
  it('should return the correct value', () => {
    expect(makeEventSelector(mockLaoId, mockRollCallState.id)(filledState)).toEqual({
      eventType: RollCall.EVENT_TYPE,
      id: mockRollCallState.id,
      idAlias: mockRollCallWithAliasState.idAlias,
    });
  });

  it('should throw an error if the given laoId is not stored', () => {
    expect(() =>
      makeEventSelector(mockLaoId, mockRollCallState.id)({ byLaoId: {} } as EventReducerState),
    ).toThrow();
  });

  it('should return undefined if the lao does not have an event with the provided id', () => {
    expect(makeEventSelector(mockLaoId, 'some id')(filledState)).toBeUndefined();
  });
});

describe('makeEventByTypeSelector', () => {
  it('should return the correct value', () => {
    expect(makeEventByTypeSelector(RollCall.EVENT_TYPE)(filledState)).toEqual({
      [mockLaoId]: {
        [mockRollCallState.id]: {
          eventType: RollCall.EVENT_TYPE,
          id: mockRollCallState.id,
          idAlias: mockRollCallWithAliasState.idAlias,
        },
      },
      someOtherId: {
        [mockRollCallState2.id]: {
          eventType: RollCall.EVENT_TYPE,
          id: mockRollCallState2.id,
        },
      },
    });
  });
});
