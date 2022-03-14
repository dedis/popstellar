import * as functions from './functions';
import { EventsConfiguration, EventsInterface, EVENTS_FEATURE_IDENTIFIER } from './interface';
import { eventsReducer, addEvent, updateEvent, removeEvent, clearAllEvents } from './reducer';

/**
 * Configures the events feature
 */
export function configure(config: EventsConfiguration): EventsInterface {
  return {
    identifier: EVENTS_FEATURE_IDENTIFIER,
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
    context: {
      useIsLaoOrganizer: config.useIsLaoOrganizer,
    },
  };
}
