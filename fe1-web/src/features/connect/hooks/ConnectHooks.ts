import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';

import { ConnectReactContext, CONNECT_FEATURE_IDENTIFIER } from '../interface/Configuration';

export namespace ConnectHooks {
  export const useConnectContext = (): ConnectReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the connect context exists
    if (!(CONNECT_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Connect context could not be found!');
    }
    return featureContext[CONNECT_FEATURE_IDENTIFIER] as ConnectReactContext;
  };

  /**
   * Returns a function from the context that allows adding a lao server address
   * @returns A function for adding a lao address
   */
  export const useAddLaoServerAddress = () => useConnectContext().addLaoServerAddress;

  /**
   * Returns a function from the context for obtaining the channel for a given lao
   * @returns A function for getting the channel by lao id
   */
  export const useGetLaoChannel = () => useConnectContext().getLaoChannel;

  /**
   * Gets the current lao id
   * @returns The current lao id
   */
  export const useCurrentLaoId = () => useConnectContext().useCurrentLaoId;
}
