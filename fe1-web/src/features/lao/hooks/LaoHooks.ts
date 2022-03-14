import FeatureContext from 'core/contexts/FeatureContext';
import { useContext } from 'react';
import { useSelector } from 'react-redux';
import { LaoReactContext, LAO_FEATURE_IDENTIFIER } from '../interface';
import { Lao } from '../objects';
import {
  selectCurrentLao,
  selectCurrentLaoId,
  selectIsLaoOrganizer,
  selectLaosList,
  selectLaosMap,
} from '../reducer';

export namespace LaoHooks {
  /* Hooks passed by dependencies */

  export const useLaoContext = (): LaoReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the evoting context exists
    if (!(LAO_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Lao context could not be found!');
    }
    return featureContext[LAO_FEATURE_IDENTIFIER] as LaoReactContext;
  };

  /**
   * Gets the lao connection encoded in a way that can be shown in a QR code
   * @returns The encoded lao connection ready to be rendered as a QR code
   */
  export const useEncodedLaoConnectionForQRCode = (
    ...args: Parameters<LaoReactContext['encodeLaoConnectionForQRCode']>
  ) => useLaoContext().encodeLaoConnectionForQRCode(...args);

  /** Hooks defined by the lao feature */
  /**
   * Retrieves a list of all the LAOs known to the system
   */
  export const useLaoList = (): Lao[] => useSelector(selectLaosList);

  /**
   * Indicates whether we're organizers of the current LAO
   */
  export const useIsLaoOrganizer = (): boolean => useSelector(selectIsLaoOrganizer);

  /**
   * Retrieve a map of the LAOs
   */
  export const useLaoMap = (): Record<string, Lao> => useSelector(selectLaosMap);

  /**
   * Returns the current lao and throws an error if there is none
   * @returns The current lao
   */

  export const useCurrentLao = () => {
    const currentLao = useSelector(selectCurrentLao);

    if (!currentLao) {
      throw new Error('Error encountered while accessing storage : no currently opened LAO');
    }

    return currentLao;
  };

  /**
   * Returns the current lao id and throws an error if there is none
   * @returns The current lao id
   */
  export const useCurrentLaoId = () => {
    const currentLaoId = useSelector(selectCurrentLaoId);

    if (!currentLaoId) {
      throw new Error('Error encountered while accessing storage : no currently opened LAO');
    }

    return currentLaoId;
  };
}
