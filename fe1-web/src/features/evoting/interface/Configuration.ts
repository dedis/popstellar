import React from 'react';
import { AnyAction } from 'redux';

import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { EvotingFeature } from './Feature';

export const EVOTING_FEATURE_IDENTIFIER = 'evoting';

export interface EvotingConfiguration {
  // objects
  messageRegistry: MessageRegistry;

  /* LAO related functions */

  /**
   * Returns the currently active lao. Should be used outside react components
   * @returns The current lao
   */
  getCurrentLao: () => EvotingFeature.Lao;

  /**
   * Returns the currently active lao. Should be used inside react components
   * @returns The current lao
   */
  useCurrentLao: () => EvotingFeature.Lao;

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
  addEvent: (laoId: string | Hash, eventState: EvotingFeature.EventState) => AnyAction;

  /**
   * Creates a redux action for update the stored event state
   * @param laoId - The lao id where to update the event
   * @param eventState - The update event state
   */
  updateEvent: (laoId: string | Hash, eventState: EvotingFeature.EventState) => AnyAction;

  /**
   * Given the redux state and an event id, this function looks in the active
   * lao for an event with a matching id, creates an instance of the corresponding type
   * and returns it
   * @param id - The id of the event
   * @returns The event or undefined if none was found
   */
  getEventById: (id: Hash) => EvotingFeature.Event | undefined;
}

/**
 * The type of the context that is provided to react evoting components
 */
export type EvotingReactContext = Pick<
  EvotingConfiguration,
  /* lao */
  | 'useCurrentLao'
  | 'useCurrentLaoId'
  /* events */
  | 'getEventById'
  | 'addEvent'
  | 'updateEvent'
>;

/**
 * The interface the evoting feature exposes
 */
export interface EvotingInterface extends FeatureInterface {
  screens: {
    CreateElection: React.ComponentType<any>;
  };

  eventTypeComponents: {
    isOfType: (event: unknown) => boolean;
    Component: React.ComponentType<{ event: unknown; isOrganizer: boolean | null | undefined }>;
  }[];

  context: EvotingReactContext;
}
