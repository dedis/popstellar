import * as functions from './functions';
import * as screens from './screens';
import { publicComponents } from './components';
import {
  EventsCompositionConfiguration,
  EventsCompositionInterface,
  EventsInterface,
  EVENTS_FEATURE_IDENTIFIER,
} from './interface';
import { eventsReducer, addEvent, updateEvent, removeEvent, clearAllEvents } from './reducer';

/**
 * Configures the events feature
 */
export function configure(): EventsInterface {
  return {
    identifier: EVENTS_FEATURE_IDENTIFIER,
    functions,
    components: publicComponents,
    screens,
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

export function compose(config: EventsCompositionConfiguration): EventsCompositionInterface {
  return {
    identifier: EVENTS_FEATURE_IDENTIFIER,
    context: {
      useIsLaoOrganizer: config.useIsLaoOrganizer,
      eventTypeComponents: config.eventTypeComponents,
    },
  };
}
