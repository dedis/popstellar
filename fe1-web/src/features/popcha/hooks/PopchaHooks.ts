import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';

import { POPCHA_FEATURE_IDENTIFIER, PopchaReactContext } from '../interface';

export namespace PopchaHooks {
  export const usePopchaContext = (): PopchaReactContext => {
    const featureContext = useContext(FeatureContext);

    if (!(POPCHA_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('PoPcha context could not be found!');
    }
    return featureContext[POPCHA_FEATURE_IDENTIFIER] as PopchaReactContext;
  };

  export const useCurrentLaoId = () => usePopchaContext().useCurrentLaoId();

  export const useGenerateToken = () => usePopchaContext().generateToken;
}
