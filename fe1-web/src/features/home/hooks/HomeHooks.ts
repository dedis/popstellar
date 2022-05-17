import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';

import { HomeReactContext, HOME_FEATURE_IDENTIFIER } from '../interface';

export namespace HomeHooks {
  export const useHomeContext = (): HomeReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the evoting context exists
    if (!(HOME_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Home context could not be found!');
    }
    return featureContext[HOME_FEATURE_IDENTIFIER] as HomeReactContext;
  };

  /**
   * Gets the action creator to connect to a lao
   * @returns The action creator
   */
  export const useConnectToTestLao = () => useHomeContext().connectToTestLao;

  /**
   * Gets the function that sends a network request to create a new lao
   * @returns The function to create a lao
   */
  export const useRequestCreateLao = () => useHomeContext().requestCreateLao;

  /**
   * Gets the function to add a server address to a lao
   * @returns The function to add a server address
   */
  export const useAddLaoServerAddress = () => useHomeContext().addLaoServerAddress;

  /**
   * Gets the current lao list
   * @returns The current lao list
   */
  export const useLaoList = () => useHomeContext().useLaoList();

  /**
   * Gets the lao list component
   * @returns The lao list component
   */
  export const useLaoListComponent = () => useHomeContext().LaoList;

  /**
   * Gets the list of screens to be rendered in the main navigation
   * @returns The list of screens
   */
  export const useMainNavigationScreens = () => useHomeContext().mainNavigationScreens;
}
