import { useContext } from 'react';
import { useSelector } from 'react-redux';

import FeatureContext from 'core/contexts/FeatureContext';

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
    // assert that the lao context exists
    if (!(LAO_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Lao context could not be found!');
    }
    return featureContext[LAO_FEATURE_IDENTIFIER] as LaoReactContext;
  };

  /**
   * Gets the event list component
   * @returns The event list component
   */
  export const useEventList = () => useLaoContext().EventList;

  /**
   * Gets the function that can encode a lao connection ready to be rendered as a QR code
   * @returns The function to encode a lao connection
   */
  export const useEncodeLaoConnectionForQRCode = () => useLaoContext().encodeLaoConnectionForQRCode;

  /**
   * Gets the lao navigation screens
   * @returns The lao navigation screens
   */
  export const useLaoNavigationScreens = () => useLaoContext().laoNavigationScreens;

  /**
   * Gets the organizer navigation screens
   * @returns The organizer navigation screens
   */
  export const useOrganizerNavigationScreens = () => useLaoContext().organizerNavigationScreens;

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
   * Returns the current lao id or undefined if there is none
   * @returns The current lao id
   */
  export const useCurrentLaoId = () => useSelector(selectCurrentLaoId);
}
