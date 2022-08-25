import { publicComponents } from './components';
import * as functions from './functions';
import {
  EVENT_FEATURE_IDENTIFIER,
  EventCompositionConfiguration,
  EventCompositionInterface,
  EventInterface,
} from './interface';
import { addEvent, clearAllEvents, eventReducer, removeEvent, updateEvent } from './reducer';

/**
 * Configures the events feature
 */
export function configure(): EventInterface {
  return {
    identifier: EVENT_FEATURE_IDENTIFIER,
    // FIXME: Use correct typing
    // @ts-ignore
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
