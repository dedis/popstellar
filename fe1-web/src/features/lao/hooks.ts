import { useSelector } from 'react-redux';

import { Lao } from './objects';
import { makeIsLaoOrganizer, makeLaosList, makeLaosMap } from './reducer';

/**
 * Retrieves a list of all the LAOs known to the system
 */
export const useLaoList = (): Lao[] => useSelector(makeLaosList());

/**
 * Indicates whether we're organizers of the current LAO
 */
export const useIsLaoOrganizer = (): boolean => useSelector(makeIsLaoOrganizer());

/**
 *
 */
export const useLao = (): Lao => useSelector(makeLaosMap())
