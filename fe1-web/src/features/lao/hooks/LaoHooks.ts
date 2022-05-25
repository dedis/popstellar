import { useContext, useMemo } from 'react';
import { useSelector } from 'react-redux';

import FeatureContext from 'core/contexts/FeatureContext';

import { LaoReactContext, LAO_FEATURE_IDENTIFIER } from '../interface';
import { Lao } from '../objects';
import {
  makeLaoOrganizerBackendPublicKeySelector,
  selectCurrentLao,
  selectCurrentLaoId,
  selectIsLaoOrganizer,
  selectIsLaoWitness,
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
   */
  export const useEventListComponent = () => useLaoContext().EventList;

  /**
   * Gets the event create event button component
   */
  export const useCreateEventButtonComponent = () => useLaoContext().CreateEventButton;

  /**
   * Gets the function that can encode a lao connection ready to be rendered as a QR code
   * @returns The function to encode a lao connection
   */
  export const useEncodeLaoConnectionForQRCode = () => useLaoContext().encodeLaoConnectionForQRCode;

  /**
   * Gets the lao navigation screens
   */
  export const useLaoNavigationScreens = () => useLaoContext().laoNavigationScreens;

  /**
   * Gets the events navigation screens
   */
  export const useEventsNavigationScreens = () => useLaoContext().eventsNavigationScreens;

  /** Hooks defined by the lao feature */

  /**
   * Retrieves a list of all the LAOs known to the system
   */
  export const useLaoList = (): Lao[] => useSelector(selectLaosList);

  /**
   * Indicates whether we are an organizer of the current LAO
   */
  export const useIsLaoOrganizer = (): boolean => useSelector(selectIsLaoOrganizer);

  /**
   * Indicates whether we are a witness of the current LAO
   */
  export const useIsLaoWitness = (): boolean => useSelector(selectIsLaoWitness);

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

  /**
   * Returns the public key of the organizer's backend for a given lao id
   * @param laoId The lao id for which the key should be retrieved
   * @returns The public key or undefined if there is none
   */
  export const useLaoOrganizerBackendPublicKey = (laoId: string) => {
    const selector = useMemo(() => makeLaoOrganizerBackendPublicKeySelector(laoId), [laoId]);
    return useSelector(selector);
  };
}
