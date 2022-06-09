import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';

import { DigitalCashReactContext, DIGITAL_CASH_FEATURE_IDENTIFIER } from '../interface';

export namespace DigitalCashHooks {
  export const useDigitalCashContext = (): DigitalCashReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the evoting context exists
    if (!(DIGITAL_CASH_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Events context could not be found!');
    }
    return featureContext[DIGITAL_CASH_FEATURE_IDENTIFIER] as DigitalCashReactContext;
  };

  /**
   * Gets the current lao id
   */
  export const useCurrentLaoId = () => useDigitalCashContext().useCurrentLaoId();

  /**
   * Gets whether the current user is organizer of the given lao
   */
  export const useIsLaoOrganizer = (laoId: string) =>
    useDigitalCashContext().useIsLaoOrganizer(laoId);

  export const useRollCallTokensByLaoId = (laoId: string) =>
    useDigitalCashContext().useRollCallTokensByLaoId(laoId);

  export const useRollCallTokenByRollCallId = (laoId: string, rollCallId: string) =>
    useDigitalCashContext().useRollCallTokenByRollCallId(laoId, rollCallId);
}
