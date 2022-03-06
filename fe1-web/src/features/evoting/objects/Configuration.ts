import { PayloadAction } from '@reduxjs/toolkit';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash } from 'core/objects';
import { RootState } from 'core/redux';
import { EvotingFeature } from './Feature';

export interface EvotingConfiguration {
  // objects
  messageRegistry: MessageRegistry;

  /* LAO related functions */

  /**
   * Given the current redux state, this function returns the currently active lao
   * @param state - The current redux state
   * @returns The current lao or undefined if there is none
   */
  getCurrentLao: (state: RootState) => EvotingFeature.Lao | undefined;

  /**
   * Given the current redux state, this function returns the currently active lao id
   * @param state - The current redux state
   * @returns The current lao id or undefined if there is none
   */
  getCurrentLaoId: (state: RootState) => string | undefined;

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
   * @param state - The redux state where the data should be retrieved from
   * @param id - The id of the event
   * @returns The event or undefined if none was found
   */
  getEventFromId: (state: RootState, id: Hash) => EvotingFeature.Event | undefined;
}
