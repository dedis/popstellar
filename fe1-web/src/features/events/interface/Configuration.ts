import React from 'react';
import { AnyAction, Reducer } from 'redux';

import { Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { EventState } from '../objects';
import { EventReducerState, EVENT_REDUCER_PATH } from '../reducer';

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
     * Creates a selector for a two-level map from laoIds to eventIds to events
     * where all returned events have type 'eventType'
     * @param eventType The type of the events that should be returned
     * @returns A selector for a map from laoIds to a map of eventIds to events
     */
    makeEventByTypeSelector: (
      eventType: string,
    ) => (state: unknown) => Record<string, Record<string, EventState>>;

    /**
     * Creates a selector to return a specific event for given lao and event ids
     * @param laoId The id of the lao to select the event from
     * @param eventId The id of the event to return
     * @returns The selector
     */
    makeEventSelector: (
      laoId: Hash | string,
      eventId: Hash | string,
    ) => (state: unknown) => EventState | undefined;
  };

  components: {
    EventList: React.ComponentType<any>;
  };

  screens: {
    CreateEvent: React.ComponentType<any>;
  };

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
   * A hook returning the current lao id
   * @returns The current lao id
   */
  useCurrentLaoId: () => Hash | undefined;

  /**
   * Gets whether the current user is organizer of the current lao
   * @returns Whether the current user is organizer of the current lao
   */
  useIsLaoOrganizer: () => boolean;

  eventTypes: EventType[];
}

interface EventType {
  eventType: string;
  navigationNames: {
    createEvent: string;
  };
  Component: React.ComponentType<{
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
  | 'useCurrentLaoId'
  | 'useIsLaoOrganizer'
  /* other */
  | 'eventTypes'
>;

export interface EventCompositionInterface extends FeatureInterface {
  /* context */
  context: EventReactContext;
}
