/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, PayloadAction } from '@reduxjs/toolkit';

import { Hash, PublicKey } from 'core/objects';
import { RollCall } from 'features/rollCall/objects';

import { eventFromState, LaoEvent, LaoEventState } from '../objects';

/**
 * The EventReducerState stores all the Event-related information for a given LAO
 */
interface EventReducerState {
  /**
   * allIds stores the list of all known event IDs
   */
  allIds: string[];

  /**
   * idAlias stores the ID aliases.
   *
   * @remarks
   *
   * If a new message (with a new_id) changes the state of an event (with old_id),
   * this map associates new_id -> old_id.
   * This ensures that we can keep only one event in memory, with its up-to-date state,
   * but future messages can refer to new_id as needed.
   */
  idAlias: Record<string, string>;

  /**
   * byId maps an event ID to the event state itself
   */
  byId: Record<string, LaoEventState>;
}

/**
 * This is the root state for the Events Reducer
 */
export interface EventLaoReducerState {
  /**
   * byLaoId associates a given LAO ID with the full representation of its events
   */
  byLaoId: Record<string, EventReducerState>;
}

export const EVENT_REDUCER_PATH = 'events';

const initialState: EventLaoReducerState = {
  byLaoId: {
    myLaoId: {
      byId: {},
      allIds: [],
      idAlias: {},
    },
  },
};

const eventsSlice = createSlice({
  name: EVENT_REDUCER_PATH,
  initialState,
  reducers: {
    // Add a Event to the list of known Events
    addEvent: {
      prepare(laoId: Hash | string, event: LaoEventState): any {
        return { payload: { laoId: laoId.valueOf(), event: event } };
      },
      reducer(
        state,
        action: PayloadAction<{
          laoId: string;
          event: LaoEventState;
        }>,
      ) {
        const { laoId, event } = action.payload;

        // Lao not initialized, create it in the event state tree
        if (!(laoId in state.byLaoId)) {
          state.byLaoId[laoId] = {
            byId: {},
            allIds: [],
            idAlias: {},
          };
        }

        // find the index of the first element which starts later
        // (or starts at the same time and ends afterwards)
        const insertIdx = state.byLaoId[laoId].allIds.findIndex((id) => {
          const existingEvt = state.byLaoId[laoId].byId[id];
          return (
            existingEvt.start > event.start ||
            (existingEvt.start === event.start && (existingEvt.end || 0) > (event.end || 0))
          );
        });

        state.byLaoId[laoId].byId[event.id] = event;
        state.byLaoId[laoId].allIds.splice(insertIdx, 0, event.id);
        if (event.idAlias) {
          state.byLaoId[laoId].idAlias[event.idAlias] = event.id;
        }
      },
    },

    // Update an Event in the list of known Events
    updateEvent: {
      prepare(laoId: Hash | string, event: LaoEventState): any {
        return { payload: { laoId: laoId.valueOf(), event: event } };
      },
      reducer(
        state,
        action: PayloadAction<{
          laoId: string;
          event: LaoEventState;
        }>,
      ) {
        const { laoId, event } = action.payload;

        // Lao not initialized, return
        if (!(laoId in state.byLaoId)) {
          return;
        }

        const oldAlias = state.byLaoId[laoId].byId[event.id].idAlias;
        if (oldAlias) {
          delete state.byLaoId[laoId].idAlias[oldAlias];
        }

        state.byLaoId[laoId].byId[event.id] = event;
        if (event.idAlias) {
          state.byLaoId[laoId].idAlias[event.idAlias] = event.id;
        }
      },
    },

    // Remove a Event to the list of known Events
    removeEvent: {
      prepare(laoId: Hash | string, eventId: Hash | string): any {
        return { payload: { laoId: laoId.valueOf(), eventId: eventId.valueOf() } };
      },
      reducer(
        state,
        action: PayloadAction<{
          laoId: string;
          eventId: string;
        }>,
      ) {
        const { laoId, eventId } = action.payload;

        // Lao not initialized, return
        if (!(laoId in state.byLaoId)) {
          return;
        }

        delete state.byLaoId[laoId].byId[eventId];
        state.byLaoId[laoId].allIds = state.byLaoId[laoId].allIds.filter((e) => e !== eventId);
      },
    },

    // Empty the list of known Events ("reset")
    clearAllEvents: (state) => {
      state.byLaoId = {};
    },
  },
});

export const { addEvent, updateEvent, removeEvent, clearAllEvents } = eventsSlice.actions;

export const getEventsState = (state: any): EventLaoReducerState => state[EVENT_REDUCER_PATH];

/**
 * Creates a selector that returns a list of all events for a given lao id
 * @param laoId The id of the lao the events should be retrieved for
 */
export const makeEventListSelector = (laoId: string) =>
  createSelector(
    // First input: Get all events across all LAOs
    (state) => getEventsState(state),
    // Selector: returns an array of EventStates -- should it return an array of Event objects?
    (eventMap: EventLaoReducerState): LaoEvent[] => {
      if (!(laoId in eventMap.byLaoId)) {
        return [];
      }

      return eventMap.byLaoId[laoId].allIds
        .map((id): LaoEvent | undefined => eventFromState(eventMap.byLaoId[laoId].byId[id]))
        .filter((e) => !!e) as LaoEvent[];
      // need to assert that it is an Event[] because of TypeScript limitations as described here:
      // https://github.com/microsoft/TypeScript/issues/16069
    },
  );

/**
 * Creates a selector for a map from event id to event id alias.
 * @param laoId The id of the lao the selector should be created for
 */
export const makeEventAliasMapSelector = (laoId: string) =>
  createSelector(
    // First input: Get all events across all LAOs
    (state) => getEventsState(state),
    // Selector: returns a map of ids -> LaoEvents' ids
    (eventMap: EventLaoReducerState): Record<string, string> => {
      if (!(laoId in eventMap.byLaoId)) {
        return {};
      }

      return eventMap.byLaoId[laoId].idAlias;
    },
  );

/**
 * Returns a selector for a map from event id to LaoEvent within a Lao.
 * @param laoId - The id of the Lao the selector should be created for
 */
export const makeEventMapSelector = (laoId: string) =>
  createSelector(
    // First input: Get all events across all LAOs
    (state) => getEventsState(state),
    // Selector: returns a map of ids -> LaoEvents
    (eventMap: EventLaoReducerState): Record<string, LaoEvent> => {
      if (!eventMap || !(laoId in eventMap.byLaoId)) {
        return {};
      }

      const dictObj: Record<string, LaoEvent> = {};

      eventMap.byLaoId[laoId].allIds.forEach((evtId) => {
        const e = eventFromState(eventMap.byLaoId[laoId].byId[evtId]);
        if (e) {
          dictObj[evtId] = e;
        }
      });

      return dictObj;
    },
  );

/**
 * Gets a specific event within a Lao.
 *
 * @param laoId - The id of the Lao
 * @param eventId - The id of the event
 */
export const makeEventSelector = (
  laoId: Hash | string | undefined,
  eventId: Hash | string | undefined,
) => {
  const id = laoId?.valueOf();
  const evtId = eventId?.valueOf();

  return createSelector(
    // First input: Get all events across all LAOs
    (state) => getEventsState(state),
    // Selector: returns a map of ids -> LaoEvents
    (eventMap: EventLaoReducerState): LaoEvent | undefined => {
      if (!id || !eventMap.byLaoId[id]) {
        return undefined;
      }

      if (!evtId || !eventMap.byLaoId[id].byId[evtId]) {
        return undefined;
      }

      return eventFromState(eventMap.byLaoId[id].byId[evtId]);
    },
  );
};

/**
 * Returns all events of a certain type.
 *
 * @param eventType
 */
export const makeEventByTypeSelector = <T extends LaoEvent>(eventType: string) =>
  createSelector(
    // First input: Get all events across all LAOs
    (state) => getEventsState(state),
    // Selector: returns a map of ids -> LaoEvents
    (eventMap: EventLaoReducerState): Record<string, Record<string, T>> => {
      if (!eventMap || !eventMap.byLaoId) {
        return {};
      }

      const evtByLao: Record<string, Record<string, T>> = {};

      Object.entries(eventMap.byLaoId).forEach(([laoId, evtList]) => {
        evtByLao[laoId] = evtList.allIds
          .map((i) => evtList.byId[i])
          .filter((e) => e !== undefined && e.eventType === eventType)
          .map((e) => eventFromState(e))
          .filter((e) => e !== undefined)
          .map((e) => e as T)
          .reduce((acc, e) => {
            acc[e.id.valueOf()] = e;
            return acc;
          }, {} as Record<string, T>);
      });

      return evtByLao;
    },
  );

/**
 * Returns the list of attendees of a roll call.
 *
 * @param laoId - The id of the Lao
 * @param rollCallId - The id of the roll call
 */
export const makeRollCallAttendeesList = (
  laoId: Hash | string,
  rollCallId: Hash | string | undefined,
) => {
  const id = laoId.valueOf();
  const evtId = rollCallId?.valueOf();

  return createSelector(
    // First input: Get all events across all LAOs
    (state) => getEventsState(state),
    // Selector: returns a map of ids -> LaoEvents
    (eventMap: EventLaoReducerState): PublicKey[] => {
      if (!id || !eventMap.byLaoId[id]) {
        return [];
      }
      if (!evtId || !eventMap.byLaoId[id].byId[evtId]) {
        return [];
      }

      const rollCall = eventFromState(eventMap.byLaoId[id].byId[evtId]) as RollCall;
      if (!rollCall) {
        return [];
      }
      return rollCall.attendees || [];
    },
  );
};

export const eventReduce = eventsSlice.reducer;

export default {
  [EVENT_REDUCER_PATH]: eventsSlice.reducer,
};
