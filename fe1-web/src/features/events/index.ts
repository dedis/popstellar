import { publicComponents } from './components';
import * as functions from './functions';
import {
  EventCompositionConfiguration,
  EventCompositionInterface,
  EventInterface,
  EVENT_FEATURE_IDENTIFIER,
} from './interface';
import { eventReducer, addEvent, updateEvent, removeEvent, clearAllEvents } from './reducer';
import * as screens from './screens';

/**
 * Configures the events feature
 */
export function configure(): EventInterface {
  return {
    identifier: EVENT_FEATURE_IDENTIFIER,
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
      ...eventReducer,
    },
  };
}

export function compose(config: EventCompositionConfiguration): EventCompositionInterface {
  return {
    identifier: EVENT_FEATURE_IDENTIFIER,
    context: {
      useCurrentLaoId: config.useCurrentLaoId,
      useIsLaoOrganizer: config.useIsLaoOrganizer,
      eventTypes: config.eventTypes,
    },
  };
}
