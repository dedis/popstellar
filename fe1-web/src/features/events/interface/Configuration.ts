import React from 'react';
import { AnyAction, Reducer } from 'redux';

import { Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';
import STRINGS from 'resources/strings';

import { EventState } from '../objects';
import { EVENT_REDUCER_PATH, EventReducerState } from '../reducer';
import { EventFeature } from './Feature';

export const EVENT_FEATURE_IDENTIFIER = 'event';

/**
 * The interface the events feature exposes
 */
export interface EventInterface extends FeatureInterface {
  functions: {
    /**
     * Gets an event by its id
     * @param id The event id
     * @returns The event or undefined if none was found
     */
    getEventById: (id: Hash) => EventState | undefined;

    /**
     * Creates a selector for a map from eventIds to events
     * where all returned events have type 'eventType' and are from the given lao
     * @param laoId The id of the lao
     * @param eventType The type of the events that should be returned
     * @returns A selector for a map from laoIds to a map of eventIds to events
     */
    makeEventByTypeSelector: (
      laoId: Hash | string,
      eventType: string,
    ) => (state: any) => Record<string, EventState>;

    /**
     * Creates a selector to return a specific event for given lao and event ids
     * @param laoId The id of the lao to select the event from
     * @param eventId The id of the event to return
     * @returns The selector
     */
    makeEventSelector: (
      laoId: Hash | string,
      eventId: Hash | string,
    ) => (state: any) => EventState | undefined;
  };

  components: {
    EventList: React.ComponentType<unknown>;
    CreateEventButton: React.VFC<unknown>;
  };

  laoEventScreens: EventFeature.LaoEventScreen[];

  actionCreators: {
    /**
     * Creates a redux action for adding an event to the event store
     * @param laoId - The lao id where to add the event
     * @param event - The event
     * @returns A redux action causing the state change
     */
    addEvent: (laoId: Hash | string, event: EventState) => AnyAction;

    /**
     * Creates a redux action for update the stored event state
     * @param event - The event
     * @returns A redux action causing the state change
     */
    updateEvent: (event: EventState) => AnyAction;

    removeEvent: (laoId: string | Hash, eventId: string | Hash) => AnyAction;
    clearAllEvents: () => AnyAction;
  };

  reducers: {
    [EVENT_REDUCER_PATH]: Reducer<EventReducerState>;
  };
}

export interface EventCompositionConfiguration {
  /**
   * Returns the currently active lao id or throws an error if there is none.
   * Should be used inside react components
   */
  useAssertCurrentLaoId: () => Hash;

  /**
   * Gets whether the current user is organizer of the current lao
   * @returns Whether the current user is organizer of the current lao
   */
  useIsLaoOrganizer: () => boolean;

  eventTypes: EventType[];
}

interface EventType {
  eventType: string;
  eventName: string;
  navigationNames: {
    createEvent:
      | typeof STRINGS.events_create_meeting
      | typeof STRINGS.events_create_roll_call
      | typeof STRINGS.events_create_election;

    screenSingle:
      | typeof STRINGS.events_view_single_meeting
      | typeof STRINGS.events_view_single_roll_call
      | typeof STRINGS.events_view_single_election;
  };
  ListItemComponent: React.ComponentType<{
    eventId: string;
    isOrganizer: boolean | null | undefined;
  }>;
}

/**
 * The type of the context that is provided to react components
 */
export type EventReactContext = Pick<
  EventCompositionConfiguration,
  /* lao */
  | 'useAssertCurrentLaoId'
  | 'useIsLaoOrganizer'
  /* other */
  | 'eventTypes'
>;

export interface EventCompositionInterface extends FeatureInterface {
  /* context */
  context: EventReactContext;
}
