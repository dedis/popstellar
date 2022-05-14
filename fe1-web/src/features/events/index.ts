import { publicComponents } from './components';
import * as functions from './functions';
import {
  EventsCompositionConfiguration,
  EventsCompositionInterface,
  EventsInterface,
  EVENTS_FEATURE_IDENTIFIER,
} from './interface';
import { eventsReducer, addEvent, updateEvent, removeEvent, clearAllEvents } from './reducer';
import * as screens from './screens';

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
      useCurrentLaoId: config.useCurrentLaoId,
      useIsLaoOrganizer: config.useIsLaoOrganizer,
      eventTypes: config.eventTypes,
    },
  };
}
