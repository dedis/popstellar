import React from 'react';
import { AnyAction, Reducer } from 'redux';

import { Hash, Timestamp } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { LaoEvent, LaoEventState } from '../objects';
import { EventLaoReducerState, EVENT_REDUCER_PATH } from '../reducer';

export const EVENTS_FEATURE_IDENTIFIER = 'events';

/**
 * The interface the evoting feature exposes
 */
export interface EventsInterface extends FeatureInterface {
  /* functions */
  functions: {
    /**
     * Gets an event by its id
     * @param id The event id
     * @returns The event or undefined if none was found
     */
    getEventById: (id: Hash) => LaoEvent | undefined;

    /**
     * Function called when the user confirms an event creation. If the end is in the past, it will tell
     * the user and cancel the creation. If the event starts more than 5 minutes in the past, it will
     * ask if it can start now. Otherwise, the event will simply be created.
     *
     * @param start - The start time of the event
     * @param end - The end time of the event
     * @param createEvent - The function which creates the event
     * @param setStartModalIsVisible - The function which sets the visibility of the modal on starting
     * time being in past
     * @param setEndModalIsVisible - The function which sets the visibility of the modal on ending time
     * being in past
     */
    onConfirmPress: (
      start: Timestamp,
      end: Timestamp,
      createEvent: Function,
      setStartModalIsVisible: Function,
      setEndModalIsVisible: Function,
    ) => void;
  };

  components: {
    EventList: React.ComponentType<any>;
  };

  screens: {
    CreateEvent: React.ComponentType<any>;
  };

  /* action creators */
  actionCreators: {
    addEvent: (laoId: string | Hash, event: LaoEventState) => AnyAction;
    updateEvent: (laoId: string | Hash, event: LaoEventState) => AnyAction;
    removeEvent: (laoId: string | Hash, eventId: string | Hash) => AnyAction;
    clearAllEvents: () => AnyAction;
  };

  /* reducers */
  reducers: {
    [EVENT_REDUCER_PATH]: Reducer<EventLaoReducerState>;
  };
}

export interface EventsCompositionConfiguration {
  /* lao */

  /**
   * Gets whether the current user is organizer of the current lao
   * @returns Whether the current user is organizer of the current lao
   */
  useIsLaoOrganizer: () => boolean;

  /* other */
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
