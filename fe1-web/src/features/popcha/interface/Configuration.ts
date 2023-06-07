import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash, PopToken } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { PopchaFeature } from './Features';

export const POPCHA_FEATURE_IDENTIFIER = 'popcha';

export interface PopchaConfiguration {
  messageRegistry: MessageRegistry;
  /**
   * Generates a long term identifier (token) for the given lao id and client id
   * @param laoId the current lao id
   * @param clientId the client id
   * @returns a promise that resolves to the token
   */
  generateToken: (laoId: Hash, clientId: Hash | undefined) => Promise<PopToken>;

  /**
   * Returns the current lao id
   */
  useCurrentLaoId: () => Hash;
}

export type PopchaReactContext = Pick<PopchaConfiguration, 'useCurrentLaoId' | 'generateToken'>;

export interface PopchaInterface extends FeatureInterface {
  laoScreens: PopchaFeature.LaoScreen[];
  context: PopchaReactContext;
}
