import { Hash } from 'core/objects';
import { getStore } from 'core/redux';

import { NoCurrentLaoError } from '../errors/NoCurrentLaoError';
import {
  makeLaoOrganizerBackendPublicKeySelector,
  selectCurrentLao,
  selectCurrentLaoId,
  selectIsLaoWitness,
  getLaoById as getLaoByIdFromState,
} from '../reducer';

/**
 * Returns the current lao and throws an error if there is none
 * @returns The current lao
 */
export const getCurrentLao = () => {
  const currentLao = selectCurrentLao(getStore().getState());

  if (!currentLao) {
    throw new NoCurrentLaoError(
      'Error encountered while accessing storage : no currently opened LAO',
    );
  }

  return currentLao;
};

/**
 * Returns the current lao and throws an error if there is none
 * @returns The current lao
 */
export const getLaoById = (laoId?: Hash) => getLaoByIdFromState(laoId, getStore().getState());

/**
 * Returns the current lao id or undefined if there is none
 * @returns The current lao id
 */
export const getCurrentLaoId = () => selectCurrentLaoId(getStore().getState());

/**
 * Returns the organizer backend's public key for a given lao
 * @param laoId The lao id
 * @returns The organizer's backend public key for the given lao or undefined if it is not known
 */
export const getLaoOrganizerBackendPublicKey = (laoId?: Hash) =>
  makeLaoOrganizerBackendPublicKeySelector(laoId)(getStore().getState());

/**
 * Returns whether the user is witness of the current lao
 */
export const isLaoWitness = () => selectIsLaoWitness(getStore().getState());

/**
 * Returns the lao organizer's public key
 * @param laoId the lao id
 */
export const getLaoOrganizer = (laoId: Hash) => getLaoById(laoId)?.organizer;
