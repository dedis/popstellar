import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';
import { Hash } from 'core/objects';

import { WalletReactContext, WALLET_FEATURE_IDENTIFIER } from '../interface';

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
   * Gets the current lao id, throws error if there is none
   */
  export const useCurrentLaoId = () => useWalletContext().useCurrentLaoId();

  /**
   * Gets the current lao, throws error if there is none
   */
  export const useCurrentLao = () => useWalletContext().useCurrentLao();

  /**
   * Returns true if currently connected to a lao, false if in offline mode
   * and undefined if there is no current lao
   */
  export const useConnectedToLao = () => useWalletContext().useConnectedToLao();

  /**
   * Gets a map from rollCall ids to rollCall instances for a given lao id
   */
  export const useRollCallsByLaoId = (laoId: Hash) => useWalletContext().useRollCallsByLaoId(laoId);

  /**
   * Gets the function for obtain roll call tokens by lao id
   */
  export const useRollCallTokensByLaoId = (laoId: Hash) =>
    useWalletContext().useRollCallTokensByLaoId(laoId);
}
