import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';
import { Hash } from 'core/objects';

import { POPCHA_FEATURE_IDENTIFIER, PoPchaReactContext } from '../interface';

export namespace PoPchaHooks {
  export const usePoPchaContext = (): PoPchaReactContext => {
    const featureContext = useContext(FeatureContext);

    if (!(POPCHA_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('PoPcha context could not be found!');
    }
    return featureContext[POPCHA_FEATURE_IDENTIFIER] as PoPchaReactContext;
  };

  export const useCurrentLaoId = () => usePoPchaContext().useCurrentLaoId();

  export const useGenerateToken = (laoId: Hash, clientId: Hash | undefined) =>
    usePoPchaContext().generateToken(laoId, clientId);
}
