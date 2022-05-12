import React from 'react';
import { AnyAction, Reducer } from 'redux';

import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash, PopToken } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { RollCallReducerState, ROLLCALL_REDUCER_PATH } from '../reducer';
import { RollCallFeature } from './Feature';

export const ROLLCALL_FEATURE_IDENTIFIER = 'rollCall';

export interface RollCallConfiguration {
  // objects
  messageRegistry: MessageRegistry;

  /* LAO related functions */

  /**
   * Gets the lao associated to the given id. Should be used outside react components
   */
  getLaoById: (id: string) => RollCallFeature.Lao | undefined;

  /**
   * Returns the currently active lao id. Should be used inside react components
   * @returns The current lao id
   */
  useCurrentLaoId: () => Hash | undefined;

  /**
   * An action cretor that sets the last roll call for a given lao
   */
  setLaoLastRollCall: (
    laoId: Hash | string,
    rollCallId: Hash | string,
    hasToken: boolean,
  ) => AnyAction;

  /* Event related functions */

  /**
   * Creates a redux action for adding an event to the event store
   * @param laoId - The lao id where to add the event
   * @param eventState - The event to add to the store
   * @returns A redux action causing the state change
   */
  addEvent: (laoId: string | Hash, eventState: RollCallFeature.EventState) => AnyAction;

  /**
   * Creates a redux action for update the stored event state
   * @param laoId - The lao id where to update the event
   * @param eventState - The update event state
   */
  updateEvent: (laoId: string | Hash, eventState: RollCallFeature.EventState) => AnyAction;

  /**
   * Given the redux state and an event id, this function looks in the active
   * lao for an event with a matching id, creates an instance of the corresponding type
   * and returns it
   * @param id - The id of the event
   * @returns The event or undefined if none was found
   */
  getEventById: (id: Hash) => RollCallFeature.Event | undefined;

  /**
   * Creates a selector to return a specific event for given lao and event ids
   * @param laoId The id of the lao to select the event from
   * @param eventId The id of the event to return
   * @returns The selector
   */
  makeEventSelector: (
    laoId: Hash | string,
    eventId: Hash | string,
  ) => (state: unknown) => RollCallFeature.Event | undefined;

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
  'useCurrentLaoId' | 'makeEventSelector' | 'generateToken' | 'hasSeed'
>;

/**
 * The interface the rollcall feature exposes
 */
export interface RollCallInterface extends FeatureInterface {
  screens: {
    CreateRollCall: React.ComponentType<any>;
    RollCallOpened: React.ComponentType<any>;
  };

  eventTypeComponents: {
    isOfType: (event: unknown) => boolean;
    Component: React.ComponentType<{ event: unknown; isOrganizer: boolean | null | undefined }>;
  }[];

  context: RollCallReactContext;

  reducers: {
    [ROLLCALL_REDUCER_PATH]: Reducer<RollCallReducerState>;
  };
}
