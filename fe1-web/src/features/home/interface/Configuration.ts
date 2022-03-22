import React from 'react';
import { AnyAction } from 'redux';

import { Channel, Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { HomeFeature } from './Feature';

export const HOME_FEATURE_IDENTIFIER = 'home';

export interface HomeCompositionConfiguration {
  /* lao */

  /* functions */
  createLao: (laoName: string) => Promise<Channel>;
  connectToTestLao: () => void;

  /* action creators */
  addLaoServerAddress: (laoId: Hash, address: string) => AnyAction;

  /* hooks */

  /**
   * Gets the current lao list
   * @returns The current lao list
   */
  useLaoList: () => HomeFeature.Lao[];

  /* components */

  /**
   * A component rendering the list of recent laos
   */
  LaoList: React.ComponentType<unknown>;

  /* other */

  /**
   * A list of screens show in the main navigations
   */
  mainNavigationScreens: HomeFeature.Screen[];
}

/**
 * The type of the context that is provided to react evoting components
 */
export type HomeReactContext = Pick<
  HomeCompositionConfiguration,
  /* lao */
  | 'createLao'
  | 'addLaoServerAddress'
  | 'connectToTestLao'
  | 'useLaoList'
  | 'LaoList'
  | 'mainNavigationScreens'
>;

/**
 * The interface the evoting feature exposes
 */
export interface HomeInterface extends FeatureInterface {
  /* navigation */
  navigation: {
    MainNavigation: React.ComponentType<any>;
  };
  screens: {
    Home: React.ComponentType<any>;
    Launch: React.ComponentType<any>;
  };
  /* context */
  context: HomeReactContext;
}
