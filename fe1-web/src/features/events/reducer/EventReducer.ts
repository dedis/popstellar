/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, PayloadAction } from '@reduxjs/toolkit';

import { Hash } from 'core/objects';

import { EventState } from '../objects';

/**
 * This is the root state for the Events Reducer
 */
export interface EventReducerState {
  /**
   * byLaoId associates a given LAO ID with the full representation of its events
   */
  byLaoId: {
    [laoId: string]: {
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
      byId: Record<string, EventState>;
    };
  };
}

export const EVENT_REDUCER_PATH = 'event';

const initialState: EventReducerState = {
  byLaoId: {},
};

const eventSlice = createSlice({
  name: EVENT_REDUCER_PATH,
  initialState,
  reducers: {
    // Add a Event to the list of known Events
    addEvent: {
      prepare(
        laoId: Hash | string,
        eventType: string,
        id: Hash | string,
        idAlias: Hash | string | undefined,
      ) {
        return {
          payload: {
            laoId: laoId.valueOf(),
            event: { eventType, id: id.valueOf(), idAlias: idAlias?.valueOf() } as EventState,
          },
        };
      },
      reducer(
        state,
        action: PayloadAction<{
          laoId: string;
          event: EventState;
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

        if (event.id in state.byLaoId[laoId].byId) {
          throw new Error(
            `Tried to store event with id ${event.id} but there already exists one with the same id`,
          );
        }

        state.byLaoId[laoId].byId[event.id] = event;
        state.byLaoId[laoId].allIds.push(event.id);
        if (event.idAlias) {
          state.byLaoId[laoId].idAlias[event.idAlias] = event.id;
        }
      },
    },

    // Update an Event in the list of known Events
    updateEvent: {
      prepare(
        laoId: Hash | string,
        eventType: string,
        id: Hash | string,
        idAlias: Hash | string | undefined,
      ) {
        return {
          payload: {
            laoId: laoId.valueOf(),
            event: { eventType, id: id.valueOf(), idAlias: idAlias?.valueOf() } as EventState,
          },
        };
      },
      reducer(
        state,
        action: PayloadAction<{
          laoId: string;
          event: EventState;
        }>,
      ) {
        const { laoId, event } = action.payload;

        // Lao not initialized, return
        if (!(laoId in state.byLaoId)) {
          throw new Error(`Tried to update event in inexistent lao with id ${laoId}`);
        }

        if (!(event.id in state.byLaoId[laoId].byId)) {
          throw new Error(`Tried to update inexistent event with id ${event.id}`);
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
          throw new Error(`Tried to remove event in inexistent lao with id ${laoId}`);
        }

        if (!(eventId in state.byLaoId[laoId].byId)) {
          throw new Error(`Tried to update inexistent event with id ${eventId}`);
        }

        const alias = state.byLaoId[laoId].byId[eventId].idAlias;
        if (alias) {
          delete state.byLaoId[laoId].idAlias[alias];
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

export const { addEvent, updateEvent, removeEvent, clearAllEvents } = eventSlice.actions;

export const getEventState = (state: any): EventReducerState => state[EVENT_REDUCER_PATH];

/**
 * Creates a selector that returns a list of all events for a given lao id
 * @param laoId The id of the lao the events should be retrieved for
 */
export const makeEventListSelector = (laoId: string) =>
  createSelector(
    // First input: Get all events across all LAOs
    (state) => getEventState(state),
    // Selector: returns an array of EventStates -- should it return an array of Event objects?
    (eventMap: EventReducerState): EventState[] => {
      if (!(laoId in eventMap.byLaoId)) {
        throw new Error(`Tried to retrive event list for inexistent lao with id ${laoId}`);
      }

      return eventMap.byLaoId[laoId].allIds.map((id) => eventMap.byLaoId[laoId].byId[id]);
    },
  );

/**
 * Creates a selector for a map from event id to event id alias.
 * @param laoId The id of the lao the selector should be created for
 */
export const makeEventAliasMapSelector = (laoId: string) =>
  createSelector(
    // First input: Get all events across all LAOs
    (state) => getEventState(state),
    // Selector: returns a map of alias ids to event ids
    (eventMap: EventReducerState): Record<string, string> => {
      if (!(laoId in eventMap.byLaoId)) {
        throw new Error(`Tried to retrive event alias map for inexistent lao with id ${laoId}`);
      }

      return eventMap.byLaoId[laoId].idAlias;
    },
  );

/**
 * Returns a selector for a map from event id to events within a lao.
 * @param laoId - The id of the Lao the selector should be created for
 */
export const makeEventMapSelector = (laoId: string) =>
  createSelector(
    // First input: Get all events across all LAOs
    (state) => getEventState(state),
    // Selector: returns a map of ids to event states
    (eventMap: EventReducerState): Record<string, EventState> => {
      if (!(laoId in eventMap.byLaoId)) {
        throw new Error(`Tried to retrive event map for inexistent lao with id ${laoId}`);
      }

      return eventMap.byLaoId[laoId].byId;
    },
  );

/**
 * Gets a specific event within a Lao.
 *
 * @param laoId - The id of the Lao
 * @param eventId - The id of the event
 */
export const makeEventSelector = (laoId: string, eventId: string) => {
  return createSelector(
    // First input: Get all events across all LAOs
    (state) => getEventState(state),
    // Selector: returns the state of a given event
    (eventMap: EventReducerState): EventState | undefined => {
      if (!(laoId in eventMap.byLaoId)) {
        throw new Error(`Tried to retrive an event for inexistent lao with id ${laoId}`);
      }

      if (!(eventId in eventMap.byLaoId[laoId].byId)) {
        return undefined;
      }

      return eventMap.byLaoId[laoId].byId[eventId];
    },
  );
};

/**
 * Returns all events of a certain type.
 *
 * @param eventType
 */
export const makeEventByTypeSelector = (eventType: string) =>
  createSelector(
    // First input: Get all events across all LAOs
    (state) => getEventState(state),
    // Selector: returns a map of lao ids to a map of event its to event states
    (eventMap: EventReducerState): Record<string, Record<string, EventState>> => {
      const eventByLao: Record<string, Record<string, EventState>> = {};

      Object.entries(eventMap.byLaoId).forEach(([laoId, eventList]) => {
        eventByLao[laoId] = eventList.allIds
          .map((i) => eventList.byId[i])
          .filter((e): e is EventState => e !== undefined && e.eventType === eventType)
          .reduce((eventById, e) => {
            eventById[e.id.valueOf()] = e;
            return eventById;
          }, {} as Record<string, EventState>);
      });

      return eventByLao;
    },
  );

export const eventReduce = eventSlice.reducer;

export default {
  [EVENT_REDUCER_PATH]: eventSlice.reducer,
};
