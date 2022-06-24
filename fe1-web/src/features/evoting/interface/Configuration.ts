import React from 'react';
import { AnyAction, Reducer } from 'redux';

import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash, PublicKey } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';
import STRINGS from 'resources/strings';

import {
  ElectionKeyReducerState,
  ELECTION_KEY_REDUCER_PATH,
  ElectionReducerState,
  ELECTION_REDUCER_PATH,
} from '../reducer';
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
   * Returns the currently active lao id or throws an error if there is none.
   * Should be used inside react components
   * @returns The current lao id
   */
  useAssertCurrentLaoId: () => Hash;

  /**
   * Given a lao id, this function returns the public key of the backend
   * @param laoId The id of the lao
   * @returns The public key of the lao organizer's backend or undefined if none is known
   */
  useLaoOrganizerBackendPublicKey: (laoId: string) => PublicKey | undefined;

  /* Event related functions */

  /**
   * Creates a redux action for adding an event to the event store
   * @param laoId - The lao id where to add the event
   * @param event - The event
   * @returns A redux action causing the state change
   */
  addEvent: (laoId: Hash | string, event: EvotingFeature.EventState) => AnyAction;

  /**
   * Creates a redux action for update the stored event state
   * @param event - The event
   * @returns A redux action causing the state change
   */
  updateEvent: (event: EvotingFeature.EventState) => AnyAction;

  /**
   * Given the redux state and an event id, this function looks in the active
   * lao for an event with a matching id, creates an instance of the corresponding type
   * and returns it
   * @param id - The id of the event
   * @returns The event or undefined if none was found
   */
  getEventById: (id: Hash) => EvotingFeature.EventState | undefined;

  /**
   * Given a lao id, this function returns the public key of the backend
   * @param laoId The id of the lao
   * @returns The public key or undefined if none is known
   */
  getLaoOrganizerBackendPublicKey: (laoId: string) => PublicKey | undefined;
}

/**
 * The type of the context that is provided to react evoting components
 */
export type EvotingReactContext = Pick<
  EvotingConfiguration,
  /* lao */
  | 'useCurrentLao'
  | 'useAssertCurrentLaoId'
  | 'useLaoOrganizerBackendPublicKey'
  /* events */
  | 'getEventById'
  | 'addEvent'
  | 'updateEvent'
>;

/**
 * The interface the evoting feature exposes
 */
export interface EvotingInterface extends FeatureInterface {
  laoEventScreens: EvotingFeature.LaoEventScreen[];

  eventTypes: EventType[];

  context: EvotingReactContext;

  reducers: {
    [ELECTION_KEY_REDUCER_PATH]: Reducer<ElectionKeyReducerState>;
    [ELECTION_REDUCER_PATH]: Reducer<ElectionReducerState>;
  };
}

interface EventType {
  eventType: string;
  eventName: string;
  navigationNames: {
    createEvent: typeof STRINGS.navigation_lao_events_create_election;
    screenSingle: typeof STRINGS.navigation_lao_events_view_single_election;
  };
  ListItemComponent: React.ComponentType<{
    eventId: string;
    isOrganizer: boolean | null | undefined;
  }>;
}
