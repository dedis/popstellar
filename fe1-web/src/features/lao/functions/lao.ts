import { Channel, channelFromIds, Hash } from 'core/objects';
import { getStore } from 'core/redux';

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
    throw new Error('Error encountered while accessing storage : no currently opened LAO');
  }

  return currentLao;
};

/**
 * Returns the current lao and throws an error if there is none
 * @returns The current lao
 */
export const getLaoById = (laoId: string) => getLaoByIdFromState(laoId, getStore().getState());

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
export const getLaoOrganizerBackendPublicKey = (laoId: string) =>
  makeLaoOrganizerBackendPublicKeySelector(laoId)(getStore().getState());

/**
 * Returns whether the user is witness of the current lao
 */
export const isLaoWitness = () => selectIsLaoWitness(getStore().getState());

/**
 * Returns the lao organizer's public key
 * @param laoId the lao id
 */
export const getLaoOrganizer = (laoId: string) => getLaoById(laoId)?.organizer;

/**
 * Get a LAOs channel by its id
 * @param laoId The id of the lao whose channel should be returned
 * @returns The channel related to the passed lao id or undefined it the lao id is invalid
 */
export function getLaoChannel(laoId: string): Channel | undefined {
  try {
    return channelFromIds(new Hash(laoId));
  } catch (error) {
    console.error(`Cannot connect to LAO '${laoId}' as it is an invalid LAO ID`, error);
  }

  return undefined;
}
