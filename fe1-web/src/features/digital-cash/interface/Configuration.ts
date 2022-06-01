import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { DigitalCashFeature } from './Feature';

export const DIGITAL_CASH_FEATURE_IDENTIFIER = 'digital-cash';

export interface DigitalCashConfiguration {
  /* objects */

  messageRegistry: MessageRegistry;

  /* lao */

  /**
   * Returns the currently active lao. Should be used outside react components
   * @returns The current lao
   */
  getCurrentLao: () => DigitalCashFeature.Lao;

  /**
   * A hook returning the current lao id
   * @returns The current lao id
   */
  useCurrentLaoId: () => Hash | undefined;

  /**
   * Returns the currently active lao id. Should be used outside react components
   * @returns The current lao or undefined if there is none.
   */
  getCurrentLaoId: () => Hash | undefined;

  /**
   * Gets whether the current user is organizer of the given lao
   */
  useIsLaoOrganizer: (laoId: string) => boolean;
}

/**
 * The type of the context that is provided to react witness components
 */
export type DigitalCashReactContext = Pick<
  DigitalCashConfiguration,
  'useCurrentLaoId' | 'useIsLaoOrganizer'
>;

/**
 * The interface the witness feature exposes
 */
export interface DigitalCashInterface extends FeatureInterface {
  walletItemGenerators: DigitalCashFeature.WalletItemGenerator[];
  walletScreens: DigitalCashFeature.WalletScreen[];

  context: DigitalCashReactContext;
}
