import { useContext, useMemo } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';

import { WalletFeature, WalletReactContext, WALLET_FEATURE_IDENTIFIER } from '../interface';

export namespace WalletHooks {
  export const useWalletContext = (): WalletReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the wallet context exists
    if (!(WALLET_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Wallet context could not be found!');
    }
    return featureContext[WALLET_FEATURE_IDENTIFIER] as WalletReactContext;
  };

  /**
   * Gets the current lao id
   * @returns The current lao id
   */
  export const useCurrentLaoId = () => useWalletContext().useCurrentLaoId();

  /**
   * Gets an event by type selector for a given event type
   */
  export const useEventByTypeSelector = <T extends WalletFeature.Event>(eventType: string) => {
    const { makeEventByTypeSelector } = useWalletContext();
    return useMemo(
      () =>
        makeEventByTypeSelector(eventType) as (state: unknown) => Record<string, Record<string, T>>,
      [makeEventByTypeSelector, eventType],
    );
  };
}
