import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Channel, Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';
import React from 'react';
import { AnyAction, Reducer } from 'redux';
import { Lao } from '../objects';
import { LaoReducerState, LAO_REDUCER_PATH } from '../reducer';
import { LaoFeature } from './Feature';

export const LAO_FEATURE_IDENTIFIER = 'lao';

export interface LaoConfiguration {
  /* other */
  registry: MessageRegistry;
}

export interface LaoCompositionConfiguration {
  /* connect */

  /**
   * Given the lao server address and the lao id, this computes the data
   * that is encoded in a QR code that can be used to connect to a LAO
   */
  encodeLaoConnectionForQRCode: (server: string, laoId: string) => string;

  /* other */

  /**
   * The screens that should additionally be included in the lao navigation
   */
  laoNavigationScreens: LaoFeature.LaoScreen[];
}

/**
 * The interface the lao feature exposes
 */
export interface LaoConfigurationInterface extends FeatureInterface {
  /* components */
  components: {
    LaoList: React.ComponentType<unknown>;
  };

  /* hooks */
  hooks: {
    /**
     * Gets the list of LAOs
     * @returns The list of LAOs
     */
    useLaoList: () => Lao[];

    /**
     * Checks whether the current user is the organizer of the current lao
     * @returns Whether the current user is the organizer of the current lao
     */
    useIsLaoOrganizer: () => boolean;

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
     * @returns The current lao id
     */
    useCurrentLaoId: () => Hash;
  };

  /* functions */
  functions: {
    /**
     * Gets the current lao
     * @returns The current lao
     */
    getCurrentLao: () => Lao;

    /**
     * Gets the current lao id
     * @returns The current lao id
     */
    getCurrentLaoId: () => Hash;

    /**
     * Creates a new lao
     */
    createLao: (laoName: string) => Promise<Channel>;

    /**
     * Opens a lao test connection
     */
    openLaoTestConnection: () => void;
  };

  /* reducers */
  reducers: {
    [LAO_REDUCER_PATH]: Reducer<LaoReducerState, AnyAction>;
  };
}

/**
 * The type of the context that is provided to react evoting components
 */
export type LaoReactContext = Pick<
  LaoCompositionConfiguration,
  /* connect */
  | 'encodeLaoConnectionForQRCode'
  /* navigation screens */
  | 'laoNavigationScreens'
>;

export interface LaoCompositionInterface extends FeatureInterface {
  /* navigation */
  navigation: {
    LaoNavigation: React.ComponentType<unknown>;
  };
  /* react context */
  context: LaoReactContext;
}
