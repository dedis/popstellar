import React from 'react';
import { AnyAction, Reducer } from 'redux';

import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash, PopToken, PublicKey, RollCallToken } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';
import STRINGS from 'resources/strings';

import { RollCall } from '../objects';
import { ROLLCALL_REDUCER_PATH, RollCallReducerState } from '../reducer';
import { RollCallFeature } from './Feature';

export const ROLLCALL_FEATURE_IDENTIFIER = 'rollCall';

export interface RollCallConfiguration {
  // objects
  messageRegistry: MessageRegistry;

  /* LAO related functions */

  /**
   * Gets the lao associated to the given id. Should be used outside react components
   */
  getLaoById: (id: Hash) => RollCallFeature.Lao | undefined;

  /**
   * Returns the currently active lao id or throws an error if there is none.
   * Should be used inside react components
   * @returns The current lao id
   */
  useCurrentLaoId: () => Hash;

  /**
   * Returns true if currently connected to a lao, false if in offline mode
   * and undefined if there is no current lao
   */
  useConnectedToLao: () => boolean | undefined;

  /**
   * An action cretor that sets the last roll call for a given lao
   */
  setLaoLastRollCall: (laoId: Hash, rollCallId: Hash, hasToken: boolean) => AnyAction;

  /* Event related functions */

  /**
   * Creates a redux action for adding an event to the event store
   * @param laoId - The lao id where to add the event
   * @param event - The event
   * @returns A redux action causing the state change
   */
  addEvent: (laoId: Hash, event: RollCallFeature.EventState) => AnyAction;

  /**
   * Creates a redux action for update the stored event state
   * @param event - The event
   * @returns A redux action causing the state change
   */
  updateEvent: (event: RollCallFeature.EventState) => AnyAction;

  /**
   * Given the redux state and an event id, this function looks in the active
   * lao for an event with a matching id, creates an instance of the corresponding type
   * and returns it
   * @param id - The id of the event
   * @returns The event or undefined if none was found
   */
  getEventById: (id: Hash) => RollCallFeature.EventState | undefined;

  /**
   * Creates a selector for a map from eventIds to events
   * where all returned events have type 'eventType'
   * @param eventType The type of the events that should be returned
   * @returns A selector for a map from laoIds to a map of eventIds to events
   */
  makeEventByTypeSelector: (
    laoId: Hash,
    eventType: string,
  ) => (state: unknown) => Record<string, RollCallFeature.EventState>;

  /**
   * Deterministically generates a pop token from given lao and rollCall ids
   * @param laoId The lao id to generate a token for
   * @param rollCallId The rollCall id to generate a token for
   * @returns The generated pop token
   */
  generateToken: (laoId: Hash, rollCallId: Hash | undefined) => Promise<PopToken>;

  /**
   * Checks whether a seed is present in the wallet store
   */
  hasSeed: () => boolean;
}

/**
 * The type of the context that is provided to react rollcall components
 */
export type RollCallReactContext = Pick<
  RollCallConfiguration,
  'useCurrentLaoId' | 'useConnectedToLao' | 'makeEventByTypeSelector' | 'generateToken' | 'hasSeed'
>;

/**
 * The interface the rollcall feature exposes
 */
export interface RollCallInterface extends FeatureInterface {
  laoEventScreens: RollCallFeature.LaoEventScreen[];

  eventTypes: EventType[];

  functions: {
    getRollCallById: (rollCallId: Hash) => RollCall | undefined;
  };

  hooks: {
    useRollCallById: (rollCallId?: Hash) => RollCall | undefined;
    useRollCallsByLaoId: (laoId: Hash) => {
      [rollCallId: string]: RollCall;
    };

    useRollCallTokensByLaoId: (laoId: Hash) => RollCallToken[];
    useRollCallTokenByRollCallId: (laoId: Hash, rollCallId?: Hash) => RollCallToken | undefined;

    useRollCallAttendeesById: (rollCallId?: Hash) => PublicKey[];
  };

  context: RollCallReactContext;

  reducers: {
    [ROLLCALL_REDUCER_PATH]: Reducer<RollCallReducerState>;
  };
}

interface EventType {
  eventType: string;
  eventName: string;
  navigationNames: {
    createEvent: typeof STRINGS.events_create_roll_call;
    screenSingle: typeof STRINGS.events_view_single_roll_call;
  };
  ListItemComponent: React.ComponentType<{
    eventId: string;
    isOrganizer: boolean | null | undefined;
  }>;
}
