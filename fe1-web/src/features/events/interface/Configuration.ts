import React from 'react';
import { AnyAction, Reducer } from 'redux';

import { Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { LaoEvent, LaoEventState } from '../objects';
import { EventLaoReducerState, EVENT_REDUCER_PATH } from '../reducer';

export const EVENTS_FEATURE_IDENTIFIER = 'events';

/**
 * The interface the events feature exposes
 */
export interface EventsInterface extends FeatureInterface {
  functions: {
    /**
     * Gets an event by its id
     * @param id The event id
     * @returns The event or undefined if none was found
     */
    getEventById: (id: Hash) => LaoEvent | undefined;
  };

  components: {
    EventList: React.ComponentType<any>;
  };

  screens: {
    CreateEvent: React.ComponentType<any>;
  };

  actionCreators: {
    addEvent: (laoId: string | Hash, event: LaoEventState) => AnyAction;
    updateEvent: (laoId: string | Hash, event: LaoEventState) => AnyAction;
    removeEvent: (laoId: string | Hash, eventId: string | Hash) => AnyAction;
    clearAllEvents: () => AnyAction;
  };

  reducers: {
    [EVENT_REDUCER_PATH]: Reducer<EventLaoReducerState>;
  };
}

export interface EventsCompositionConfiguration {
  /**
   * Gets whether the current user is organizer of the current lao
   * @returns Whether the current user is organizer of the current lao
   */
  useIsLaoOrganizer: () => boolean;

  eventTypeComponents: {
    isOfType: (event: unknown) => boolean;
    Component: React.ComponentType<{ event: unknown; isOrganizer: boolean | null | undefined }>;
  }[];
}

/**
 * The type of the context that is provided to react components
 */
export type EventsReactContext = Pick<
  EventsCompositionConfiguration,
  /* lao */
  | 'useIsLaoOrganizer'
  /* other */
  | 'eventTypeComponents'
>;

export interface EventsCompositionInterface extends FeatureInterface {
  /* context */
  context: EventsReactContext;
}
