import { getStore } from 'core/redux';
import { selectCurrentLao, selectCurrentLaoId } from '../reducer';

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
 * Returns the current lao id and throws an error if there is none
 * @returns The current lao id
 */
export const getCurrentLaoId = () => {
  const currentLaoId = selectCurrentLaoId(getStore().getState());

  if (!currentLaoId) {
    throw new Error('Error encountered while accessing storage : no currently opened LAO');
  }

  return currentLaoId;
};
