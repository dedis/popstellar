import { createSlice, createSelector, PayloadAction } from '@reduxjs/toolkit';
import {
  Hash, LaoEvent, LaoEventState, eventFromState,
} from 'model/objects';
import eventsData from 'res/EventData';
import { getLaosState } from './LaoReducer';

/**
 * Reducer & associated function implementation to store all known Events
 */

interface EventReducerState {
  byId: Record<string, LaoEventState>,
  allIds: string[],
}

interface EventLaoReducerState {
  byLaoId: Record<string, EventReducerState>,
}

const initialState: EventLaoReducerState = {
  byLaoId: {
    myLaoId: {
      byId: Object.assign({},
        ...eventsData.map((evt: LaoEventState) => ({
          [evt.id]: evt,
        }))),
      allIds: eventsData.map((evt) => evt.id),
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
          };
        }

        // find the index of the first element which starts later
        // (or starts at the same time and ends afterwards)
        const insertIdx = state.byLaoId[laoId].allIds.findIndex((id) => (
          state.byLaoId[laoId].byId[id].start > event.start
          || (state.byLaoId[laoId].byId[id].start === event.start
            && state.byLaoId[laoId].byId[id].end > event.end
          )));

        state.byLaoId[laoId].byId[event.id] = event;
        state.byLaoId[laoId].allIds.splice(insertIdx, 0, event.id);
      },
    },

    // Update an Event in the list of known Events
    updateEvent: {
      prepare(laoId: Hash | string, event: LaoEventState): any {
        return { payload: { laoId: laoId.valueOf(), event: event } };
      },
      reducer(state, action: PayloadAction<{
        laoId: string;
        event: LaoEventState;
      }>) {
        const { laoId /* , event */ } = action.payload;

        // Lao not initialized, return
        if (!(laoId in state.byLaoId)) {
          return;
        }

        // TODO: to be implemented
        console.error('events/UpdateEvent is not implemented');
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

        // Lao not initialized, create it in the event state tree
        if (!(laoId in state.byLaoId)) {
          return;
        }

        delete state.byLaoId[laoId].byId[eventId];
        state.byLaoId[laoId].allIds.filter((e) => e !== eventId);
      },
    },

    // Empty the list of known Events ("reset")
    clearAllEvents: (state) => {
      state.byLaoId = {};
    },
  },
});

export const {
  addEvent, removeEvent, clearAllEvents,
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
    if (!laoId || !Object.prototype.hasOwnProperty.call(eventMap.byLaoId, laoId)) {
      return [];
    }

    return eventMap.byLaoId[laoId].allIds
      .map((id) : LaoEvent | undefined => eventFromState(eventMap.byLaoId[laoId].byId[id]))
      .filter((e) => e && Object.keys(e).length) as LaoEvent[];
    // need to assert that it is an Event[] because of TypeScript limitations as described here:
    // https://github.com/microsoft/TypeScript/issues/16069
  },
);

export const makeEventsMap = () => createSelector(
  // First input: Get all events across all LAOs
  (state) => getEventsState(state),
  // Second input: get the current LAO id,
  (state) => getLaosState(state).currentId,
  // Selector: returns an array of EventStates -- should it return an array of Event objects?
  (eventMap: EventLaoReducerState, laoId: string | undefined)
  : Record<string, LaoEvent> => {
    if (!laoId) {
      return {};
    }

    const dictObj: Record<string, LaoEvent> = {};

    eventMap.byLaoId[laoId].allIds.forEach((id) => {
      const e = eventFromState(eventMap.byLaoId[laoId].byId[id]);
      if (e) {
        dictObj[id] = e;
      }
    });

    return dictObj;
  },
);

export default {
  [eventReducerPath]: eventsSlice.reducer,
};
