import { useContext } from 'react';

import FeatureContext from '../../../core/contexts/FeatureContext';
import { POPCHA_FEATURE_IDENTIFIER, PoPchaReactContext } from '../interface';

export namespace PoPchaHooks {
  export const usePoPchaContext = (): PoPchaReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the social context exists
    if (!(POPCHA_FEATURE_IDENTIFIER in featureContext)) {
      console.log('featureContext: ', featureContext);
      throw new Error('PoPcha context could not be found!');
    }
    return featureContext[POPCHA_FEATURE_IDENTIFIER] as PoPchaReactContext;
  };

  export const useCurrentLaoId = () => usePoPchaContext().useCurrentLaoId();
}