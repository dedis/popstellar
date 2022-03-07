import { getEvotingConfig } from '../index';

export namespace EvotingHooks {
  /**
   * Gets the current lao id
   * @returns The current lao id
   */
  export const useCurrentLaoId = () => {
    return getEvotingConfig().getCurrentLaoId();
  };

  /**
   * Gets the current lao
   * @returns The current lao
   */
  export const useCurrentLao = () => {
    return getEvotingConfig().getCurrentLao();
  };

  /**
   * Gets the onConfirmEventCreation helper function
   * @returns The onConfirmEventCreation function
   */
  export const useOnConfirmEventCreation = () => {
    return getEvotingConfig().onConfirmEventCreation;
  };
}
