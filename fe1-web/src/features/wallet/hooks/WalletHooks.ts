import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';

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
   * Gets the list of wallet item generators
   */
  export const useWalletItemGenerators = () => useWalletContext().walletItemGenerators;

  /**
   * Gets the current lao id
   */
  export const useCurrentLaoId = () => useWalletContext().useCurrentLaoId();

  /**
   * Returns true if currently connected to a lao, false if in offline mode
   * and undefined if there is no current lao
   */
  export const useConnectedToLao = () => useWalletContext().useConnectedToLao();

  /**
   * Gets all lao ids
   */
  export const useLaoIds = () => useWalletContext().useLaoIds();

  /**
   * Gets a map from rollCall ids to rollCall instances for a given lao id
   */
  export const useRollCallsByLaoId = (laoId: string) =>
    useWalletContext().useRollCallsByLaoId(laoId);

  /**
   * Gets the function for obtain roll call tokens by lao id
   */
  export const useRollCallTokensByLaoId = (laoId: string) =>
    useWalletContext().useRollCallTokensByLaoId(laoId);

  /**
   * Gets a map from laoIds to names
   */
  export const useNamesByLaoId = () => useWalletContext().useNamesByLaoId();

  /**
   * Gets the list of wallet navigation screens
   */
  export const useWalletNavigationScreens = () => useWalletContext().walletNavigationScreens;
}
