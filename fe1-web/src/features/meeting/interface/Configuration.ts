import React from 'react';
import { AnyAction, Reducer } from 'redux';

import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';
import STRINGS from 'resources/strings';

import { MeetingReducerState } from '../reducer';
import { MeetingFeature } from './Feature';

export const MEETING_FEATURE_IDENTIFIER = 'meeting';

export interface MeetingConfiguration {
  // objects
  messageRegistry: MessageRegistry;

  /* LAO related functions */

  /**
   * Gets the lao associated to the given id. Should be used outside react components
   */
  getLaoById: (id: Hash) => MeetingFeature.Lao | undefined;

  /**
   * Returns the currently active lao id. Should be used inside react components
   * @returns The current lao id
   */
  useCurrentLaoId: () => Hash;

  /**
   * Returns true if currently connected to a lao, false if in offline mode
   * and undefined if there is no current lao
   */
  useConnectedToLao: () => boolean | undefined;

  /* Event related functions */

  /**
   * Creates a redux action for adding an event to the event store
   * @param laoId - The lao id where to add the event
   * @param event - The event
   * @returns A redux action causing the state change
   */
  addEvent: (laoId: Hash, event: MeetingFeature.EventState) => AnyAction;

  /**
   * Creates a redux action for update the stored event state
   * @param laoId - The lao id where to add the event
   * @param event - The event
   * @returns A redux action causing the state change
   */
  updateEvent: (event: MeetingFeature.EventState) => AnyAction;

  /**
   * Given the redux state and an event id, this function looks in the active
   * lao for an event with a matching id, creates an instance of the corresponding type
   * and returns it
   * @param id - The id of the event
   * @returns The event or undefined if none was found
   */
  getEventById: (id: Hash) => MeetingFeature.EventState | undefined;
}

/**
 * The type of the context that is provided to react meeting components
 */
export type MeetingReactContext = Pick<
  MeetingConfiguration,
  'useCurrentLaoId' | 'useConnectedToLao'
>;

/**
 * The interface the meeting feature exposes
 */
export interface MeetingInterface extends FeatureInterface {
  laoEventScreens: MeetingFeature.LaoEventScreen[];

  eventTypes: EventType[];

  context: MeetingReactContext;

  reducers: {
    [MEETING_FEATURE_IDENTIFIER]: Reducer<MeetingReducerState>;
  };
}

interface EventType {
  eventType: string;
  eventName: string;
  navigationNames: {
    createEvent: typeof STRINGS.events_create_meeting;
    screenSingle: typeof STRINGS.events_view_single_meeting;
  };
  ListItemComponent: React.ComponentType<{
    eventId: string;
    isOrganizer: boolean | null | undefined;
  }>;
}
