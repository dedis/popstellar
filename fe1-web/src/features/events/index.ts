import * as functions from './functions';
import { eventsReducer, addEvent, updateEvent, removeEvent, clearAllEvents } from './reducer';

/**
 * Configures the events feature
 */
export function configure() {
  return {
    functions,
    actionCreators: {
      addEvent,
      updateEvent,
      removeEvent,
      clearAllEvents,
    },
    reducers: {
      ...eventsReducer,
    },
  };
}
