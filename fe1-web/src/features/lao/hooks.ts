import { useSelector } from 'react-redux';
import { useMemo } from 'react';

import { Lao } from './objects';
import { makeIsLaoOrganizer, makeLaosList, makeLaosMap } from './reducer';

/**
 * Retrieves a list of all the LAOs known to the system
 */
export const useLaoList = (): Lao[] => {
  const laosList = useMemo(makeLaosList, []);
  return useSelector(laosList);
};

/**
 * Indicates whether we're organizers of the current LAO
 */
export const useIsLaoOrganizer = (): boolean => {
  const isLaoOrg = useMemo(makeIsLaoOrganizer, []);
  return useSelector(isLaoOrg);
};

/**
 * Retrieve a map of the LAOs
 */
export const useLaoMap = (): Record<string, Lao> => {
  const laosMap = useMemo(makeLaosMap, []);
  return useSelector(laosMap);
};
