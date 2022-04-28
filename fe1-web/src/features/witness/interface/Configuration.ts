import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { WitnessFeature } from './Feature';

export const WITNESS_FEATURE_IDENTIFIER = 'witness';

export interface WitnessConfiguration {
  /* feature toggle */
  enabled: boolean;

  /* objects */

  messageRegistry: MessageRegistry;

  /* lao */

  /**
   * Returns the currently active lao. Should be used outside react components
   * @returns The current lao
   */
  getCurrentLao: () => WitnessFeature.Lao;

  /**
   * Returns the currently active lao id. Should be used outside react components
   * @returns The current lao  or undefined if there is none.
   */
  getCurrentLaoId: () => Hash | undefined;

  /**
   * Returns whether the user is witness of the current lao
   */
  isLaoWitness: () => boolean;

  /**
   * Returns the currently active lao. Can only be used inside react components
   * @returns The current lao
   */
  useCurrentLao: () => WitnessFeature.Lao;
}

/**
 * The type of the context that is provided to react witness components
 */
export type WintessReactContext = Pick<WitnessConfiguration, 'useCurrentLao'>;

/**
 * The interface the witness feature exposes
 */
export interface WitnessInterface extends FeatureInterface {
  context: WintessReactContext;
}
