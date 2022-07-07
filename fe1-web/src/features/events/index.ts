import { publicComponents } from './components';
import * as functions from './functions';
import {
  EventCompositionConfiguration,
  EventCompositionInterface,
  EventInterface,
  EVENT_FEATURE_IDENTIFIER,
} from './interface';
import { eventReducer, addEvent, updateEvent, removeEvent, clearAllEvents } from './reducer';

/**
 * Configures the events feature
 */
export function configure(): EventInterface {
  return {
    identifier: EVENT_FEATURE_IDENTIFIER,
    functions,
    components: publicComponents,
    actionCreators: {
      addEvent,
      updateEvent,
      removeEvent,
      clearAllEvents,
    },
    reducers: {
      ...eventReducer,
    },
  };
}

export function compose(config: EventCompositionConfiguration): EventCompositionInterface {
  return {
    identifier: EVENT_FEATURE_IDENTIFIER,
    context: {
      useAssertCurrentLaoId: config.useAssertCurrentLaoId,
      useIsLaoOrganizer: config.useIsLaoOrganizer,
      eventTypes: config.eventTypes,
    },
  };
}
