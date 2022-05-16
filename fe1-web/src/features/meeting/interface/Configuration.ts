import React from 'react';
import { AnyAction } from 'redux';

import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { MeetingFeature } from './Feature';

export const MEETING_FEATURE_IDENTIFIER = 'meeting';

export interface MeetingConfiguration {
  // objects
  messageRegistry: MessageRegistry;

  /* LAO related functions */

  /**
   * Gets the lao associated to the given id. Should be used outside react components
   */
  getLaoById: (id: string) => MeetingFeature.Lao | undefined;

  /**
   * Returns the currently active lao id. Should be used inside react components
   * @returns The current lao id
   */
  useCurrentLaoId: () => Hash | undefined;

  /* Event related functions */

  /**
   * Creates a redux action for adding an event to the event store
   * @param laoId - The lao id where to add the event
   * @param eventState - The event to add to the store
   * @returns A redux action causing the state change
   */
  addEvent: (laoId: string | Hash, eventState: MeetingFeature.EventState) => AnyAction;

  /**
   * Creates a redux action for update the stored event state
   * @param laoId - The lao id where to update the event
   * @param eventState - The update event state
   */
  updateEvent: (laoId: string | Hash, eventState: MeetingFeature.EventState) => AnyAction;

  /**
   * Given the redux state and an event id, this function looks in the active
   * lao for an event with a matching id, creates an instance of the corresponding type
   * and returns it
   * @param id - The id of the event
   * @returns The event or undefined if none was found
   */
  getEventById: (id: Hash) => MeetingFeature.Event | undefined;
}

/**
 * The type of the context that is provided to react meeting components
 */
export type MeetingReactContext = Pick<MeetingConfiguration, 'useCurrentLaoId'>;

/**
 * The interface the meeting feature exposes
 */
export interface MeetingInterface extends FeatureInterface {
  screens: {
    CreateMeeting: React.ComponentType<any>;
  };

  eventTypeComponents: {
    isOfType: (event: unknown) => boolean;
    Component: React.ComponentType<{ event: unknown; isOrganizer: boolean | null | undefined }>;
  }[];

  context: MeetingReactContext;
}
