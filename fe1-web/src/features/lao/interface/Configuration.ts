import React from 'react';
import { AnyAction, Dispatch, Reducer } from 'redux';

import { AppScreen } from 'core/navigation/AppNavigation';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { NetworkConnection } from 'core/network/NetworkConnection';
import { Channel, Hash, PublicKey } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { Lao } from '../objects';
import { LaoReducerState, LAO_REDUCER_PATH } from '../reducer';
import { LaoFeature } from './Feature';

export const LAO_FEATURE_IDENTIFIER = 'lao';

export interface LaoConfiguration {
  /* other */
  registry: MessageRegistry;
}

export interface LaoCompositionConfiguration {
  /* events */

  EventList: React.ComponentType<unknown>;

  CreateEventButton: React.VFC<unknown>;

  /* connect */

  /**
   * Given the lao server address and the lao id, this computes the data
   * that is encoded in a QR code that can be used to connect to a LAO
   */
  encodeLaoConnectionForQRCode: (servers: string[], laoId: string) => string;

  /* other */

  /**
   * The screens that should additionally be included in the lao navigation
   */
  laoNavigationScreens: LaoFeature.LaoScreen[];

  /**
   * The screens that should additionally be included in the lao events navigation
   */
  eventsNavigationScreens: LaoFeature.LaoEventScreen[];
}

/**
 * The interface the lao feature exposes
 */
export interface LaoConfigurationInterface extends FeatureInterface {
  /* components */
  components: {
    LaoList: React.ComponentType<unknown>;
  };

  /* action creators */
  actionCreators: {
    /**
     * Creates a redux action to add a server address for a given lao
     */
    addLaoServerAddress: (laoId: Hash | string, serverAddress: string) => AnyAction;

    /**
     * Creates a redux action to set the last roll call for a given lao
     */
    setLaoLastRollCall: (
      laoId: Hash | string,
      rollCallId: Hash | string,
      hasToken: boolean,
    ) => AnyAction;
  };

  /* hooks */
  hooks: {
    /**
     * Gets the list of laos
     */
    useLaoList: () => Lao[];

    /**
     * Gets the list of lao ids
     */
    useLaoIds: () => Hash[];

    /**
     * Checks whether the current user is an organizer of the given lao
     * If no laoId is passed, it is checked for the current lao
     */
    useIsLaoOrganizer: (laoId?: string) => boolean;

    /**
     * Checks whether the current user is a witness of the current lao
     * @returns Whether the current user is a witness of the current lao
     */
    useIsLaoWitness: () => boolean;

    /**
     * Gets the map of lao ids to the respective laos
     * @returns The map of ids to laos
     */
    useLaoMap: () => Record<string, Lao>;

    /**
     * Gets the current lao
     * @returns The current lao
     */
    useCurrentLao: () => Lao;

    /**
     * Gets the current lao id
     * @returns The current lao id or undefined if there is none
     */
    useCurrentLaoId: () => Hash | undefined;

    /**
     * Returns the public key of the organizer's backend for a given lao id
     * @param laoId The lao id for which the key should be retrieved
     * @returns The public key or undefined if there is none
     */
    useLaoOrganizerBackendPublicKey: (laoId: string) => PublicKey | undefined;

    /**
     * Returns the function to disconnect from the current lao
     */
    useDisconnectFromLao: () => () => void;

    /**
     * Returns a map from lao id to the respective name
     */
    useNamesByLaoId: () => { [laoId: string]: string };
  };

  /* functions */
  functions: {
    /**
     * Gets the current lao
     * @returns The current lao
     */
    getCurrentLao: () => Lao;

    /**
     * Gets a lao by its id
     * @returns The lao with the given id or undefined if there is none
     */
    getLaoById: (laoId: string) => Lao | undefined;

    /**
     * Gets the current lao id
     * @returns The current lao id or undefined if there is none
     */
    getCurrentLaoId: () => Hash | undefined;

    /**
     * Gets the organizer backend's public key for a given lao
     * @param laoId The lao id
     * @returns The organizer's backend public key for the given lao or undefined if it is not known
     */
    getLaoOrganizerBackendPublicKey: (laoId: string) => PublicKey | undefined;

    /**
     * Sends a network request to create a new lao and returns
     * the corresponding channel
     * @returns The channel for the newly created lao
     */
    requestCreateLao: (laoName: string) => Promise<Channel>;

    /**
     * Opens a lao test connection
     */
    openLaoTestConnection: () => void;

    /**
     * Returns whether the user is witness of the current lao
     */
    isLaoWitness: () => boolean;

    /**
     * Returns the lao organizer's public key
     */
    getLaoOrganizer: (laoId: string) => PublicKey | undefined;

    /**
     * Get a LAOs channel by its id
     * @param laoId The id of the lao whose channel should be returned
     * @returns The channel related to the passed lao id
     */
    getLaoChannel: (laoId: string) => Channel | undefined;

    /**
     * Resubscribes to a known lao
     */
    resubscribeToLao: (
      lao: Lao,
      dispatch: Dispatch,
      connections?: NetworkConnection[],
    ) => Promise<void>;
  };

  /* reducers */
  reducers: {
    [LAO_REDUCER_PATH]: Reducer<LaoReducerState>;
  };
}

/**
 * The type of the context that is provided to react lao components
 */
export type LaoReactContext = Pick<
  LaoCompositionConfiguration,
  /* events */
  | 'EventList'
  | 'CreateEventButton'
  /* connect */
  | 'encodeLaoConnectionForQRCode'
  /* navigation screens */
  | 'laoNavigationScreens'
  | 'eventsNavigationScreens'
>;

export interface LaoCompositionInterface extends FeatureInterface {
  appScreens: AppScreen[];

  context: LaoReactContext;
}
