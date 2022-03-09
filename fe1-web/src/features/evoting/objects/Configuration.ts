import { PayloadAction } from '@reduxjs/toolkit';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash, Timestamp } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { EvotingFeature } from './Feature';

export interface EvotingConfiguration {
  // objects
  messageRegistry: MessageRegistry;

  /* LAO related functions */

  /**
   * Given the current redux state, this function returns the currently active lao
   * @returns The current lao
   */
  getCurrentLao: () => EvotingFeature.Lao;

  /**
   * Given the current redux state, this function returns the currently active lao id
   * @returns The current lao id
   */
  getCurrentLaoId: () => Hash;

  /* Event related functions */

  /**
   * Creates a redux action for adding an event to the event store
   * @param laoId - The lao id where to add the event
   * @param eventState - The event to add to the store
   * @returns A redux action causing the state change
   */
  addEvent: (laoId: string | Hash, eventState: EvotingFeature.EventState) => PayloadAction<unknown>;

  /**
   * Creates a redux action for update the stored event state
   * @param laoId - The lao id where to update the event
   * @param eventState - The update event state
   */
  updateEvent: (
    laoId: string | Hash,
    eventState: EvotingFeature.EventState,
  ) => PayloadAction<unknown>;

  /**
   * Given the redux state and an event id, this function looks in the active
   * lao for an event with a matching id, creates an instance of the corresponding type
   * and returns it
   * @param id - The id of the event
   * @returns The event or undefined if none was found
   */
  getEventFromId: (id: Hash) => EvotingFeature.Event | undefined;

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
  onConfirmEventCreation: (
    start: Timestamp,
    end: Timestamp,
    createEvent: Function,
    setStartModalIsVisible: Function,
    setEndModalIsVisible: Function,
  ) => void;
}

/**
 * The type of the context that is provided to react evoting components
 */
export type EvotingReactContext = Pick<
  EvotingConfiguration,
  /* lao */
  | 'getCurrentLao'
  | 'getCurrentLaoId'
  /* events */
  | 'getEventFromId'
  | 'addEvent'
  | 'updateEvent'
  | 'onConfirmEventCreation'
>;

/**
 * The interface the evoting feature exposes
 */
export interface EvotingInterface extends FeatureInterface {
  context: EvotingReactContext;
}
