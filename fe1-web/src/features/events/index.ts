import { publicComponents } from './components';
import * as functions from './functions';
import {
  EVENT_FEATURE_IDENTIFIER,
  EventCompositionConfiguration,
  EventCompositionInterface,
  EventInterface,
} from './interface';
import { addEvent, clearAllEvents, eventReducer, removeEvent, updateEvent } from './reducer';
import { UpcomingEventsScreen } from './screens/UpcomingEvents';

/**
 * Configures the events feature
 */
export function configure(): EventInterface {
  return {
    identifier: EVENT_FEATURE_IDENTIFIER,
    functions,
    components: publicComponents,
    laoEventScreens: [UpcomingEventsScreen],
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
