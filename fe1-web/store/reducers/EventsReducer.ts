import { createSlice, createSelector, PayloadAction } from '@reduxjs/toolkit';
import {
  Hash, LaoEvent, LaoEventState, eventFromState, RollCall, PublicKey,
} from 'model/objects';
// import eventsData from 'res/EventData';
import { getLaosState } from './LaoReducer';

/**
 * The EventReducerState stores all the Event-related information for a given LAO
 */
interface EventReducerState {
  /**
   * allIds stores the list of all known event IDs
   */
  allIds: string[],

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
  idAlias: Record<string, string>,

  /**
   * byId maps an event ID to the event state itself
   */
  byId: Record<string, LaoEventState>,
}

/**
 * This is the root state for the Events Reducer
 */
interface EventLaoReducerState {
  /**
   * byLaoId associates a given LAO ID with the full representation of its events
   */
  byLaoId: Record<string, EventReducerState>,
}

const initialState: EventLaoReducerState = {
  byLaoId: {
    myLaoId: {
      byId: {},
      allIds: [],
      idAlias: {},
    },
  },
};

const eventReducerPath = 'events';
const eventsSlice = createSlice({
  name: eventReducerPath,
  initialState,
  reducers: {

    // Add a Event to the list of known Events
    addEvent: {
      prepare(laoId: Hash | string, event: LaoEventState): any {
        return { payload: { laoId: laoId.valueOf(), event: event } };
      },
      reducer(state, action: PayloadAction<{
        laoId: string;
        event: LaoEventState;
      }>) {
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
          return existingEvt.start > event.start
            || (existingEvt.start === event.start && (existingEvt.end || 0) > (event.end || 0));
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
      prepare(laoId: Hash | string, event: LaoEventState, alias?: Hash | string): any {
        return { payload: { laoId: laoId.valueOf(), event: event, alias: alias?.valueOf() } };
      },
      reducer(state, action: PayloadAction<{
        laoId: string;
        event: LaoEventState;
        alias?: string;
      }>) {
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
      reducer(state, action: PayloadAction<{
        laoId: string;
        eventId: string;
      }>) {
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

export const {
  addEvent, updateEvent, removeEvent, clearAllEvents,
} = eventsSlice.actions;

export const getEventsState = (state: any): EventLaoReducerState => state[eventReducerPath];

export const makeEventsList = () => createSelector(
  // First input: Get all events across all LAOs
  (state) => getEventsState(state),
  // Second input: get the current LAO id,
  (state) => getLaosState(state).currentId,
  // Selector: returns an array of EventStates -- should it return an array of Event objects?
  (eventMap: EventLaoReducerState, laoId: string | undefined)
  : LaoEvent[] => {
    if (!laoId || !(laoId in eventMap.byLaoId)) {
      return [];
    }

    return eventMap.byLaoId[laoId].allIds
      .map((id): LaoEvent | undefined => eventFromState(eventMap.byLaoId[laoId].byId[id]))
      .filter((e) => !!e) as LaoEvent[];
    // need to assert that it is an Event[] because of TypeScript limitations as described here:
    // https://github.com/microsoft/TypeScript/issues/16069
  },
);

export const makeEventsAliasMap = () => createSelector(
  // First input: Get all events across all LAOs
  (state) => getEventsState(state),
  // Second input: get the current LAO id,
  (state) => getLaosState(state).currentId,
  // Selector: returns a map of ids -> LaoEvents' ids
  (eventMap: EventLaoReducerState, laoId: string | undefined)
  : Record<string, string> => {
    if (!laoId) {
      return {};
    }

    return eventMap.byLaoId[laoId].idAlias;
  },
);

export const makeEventsMap = (laoId: string | undefined = undefined) => createSelector(
  // First input: Get all events across all LAOs
  (state) => getEventsState(state),
  // Second input: get the current LAO id,
  (state) => laoId || getLaosState(state).currentId,
  // Selector: returns a map of ids -> LaoEvents
  (eventMap: EventLaoReducerState, id: string | undefined)
  : Record<string, LaoEvent> => {
    if (!id) {
      return {};
    }

    const dictObj: Record<string, LaoEvent> = {};

    eventMap.byLaoId[id].allIds.forEach((evtId) => {
      const e = eventFromState(eventMap.byLaoId[id].byId[evtId]);
      if (e) {
        dictObj[evtId] = e;
      }
    });

    return dictObj;
  },
);

export const makeEventGetter = (laoId: Hash | string, eventId: Hash | string | undefined) => {
  const id = laoId.valueOf();
  const evtId = eventId?.valueOf();

  return createSelector(
    // First input: Get all events across all LAOs
    (state) => getEventsState(state),
    // Selector: returns a map of ids -> LaoEvents
    (eventMap: EventLaoReducerState)
    : LaoEvent | undefined => {
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

export const makeEventByTypeSelector = <T extends LaoEvent>(eventType: string) => createSelector(
  // First input: Get all events across all LAOs
  (state) => getEventsState(state),
  // Selector: returns a map of ids -> LaoEvents
  (eventMap: EventLaoReducerState)
  : Record<string, Record<string, T>> => {
    const evtByLao: Record<string, Record<string, T>> = {};

    Object.entries(eventMap.byLaoId).forEach(
      ([laoId, evtList]) => {
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
      },
    );

    return evtByLao;
  },
);

export const makeLastRollCallAttendeesList = (laoId: Hash | string,
  rollCallId: Hash | string | undefined) => {
  const id = laoId.valueOf();
  const evtId = rollCallId?.valueOf();

  return createSelector(
    // First input: Get all events across all LAOs
    (state) => getEventsState(state),
    // Selector: returns a map of ids -> LaoEvents
    (eventMap: EventLaoReducerState) : PublicKey[] => {
      if (!id || !eventMap.byLaoId[id]) {
        console.log('Undefined LAO id');
        return [];
      }
      if (!evtId || !eventMap.byLaoId[id].byId[evtId]) {
        console.log('Undefined roll call id');
        return [];
      }

      const rollCall = eventFromState(eventMap.byLaoId[id].byId[evtId]) as RollCall;
      if (!rollCall) {
        console.log('Roll call not found');
        return [];
      }
      return rollCall.attendees ? rollCall.attendees : [];
    },
  );
};

export default {
  [eventReducerPath]: eventsSlice.reducer,
};
