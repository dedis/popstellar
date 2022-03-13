import { useSelector } from 'react-redux';

import { Lao } from './objects';
import {
  selectIsLaoOrganizer,
  selectLaosList,
  selectLaosMap,
  selectCurrentLao,
  selectCurrentLaoId,
} from './reducer';

/**
 * Retrieves a list of all the LAOs known to the system
 */
export const useLaoList = (): Lao[] => {
  return useSelector(selectLaosList);
};

/**
 * Indicates whether we're organizers of the current LAO
 */
export const useIsLaoOrganizer = (): boolean => {
  return useSelector(selectIsLaoOrganizer);
};

/**
 * Retrieve a map of the LAOs
 */
export const useLaoMap = (): Record<string, Lao> => {
  return useSelector(selectLaosMap);
};

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
