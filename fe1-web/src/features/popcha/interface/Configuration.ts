import { Hash, PopToken } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { PoPchaFeature } from './Features';

export const POPCHA_FEATURE_IDENTIFIER = 'popcha';

export interface PoPchaConfiguration {
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

export type PoPchaReactContext = Pick<PoPchaConfiguration, 'useCurrentLaoId' | 'generateToken'>;

export interface PoPchaInterface extends FeatureInterface {
  laoScreens: PoPchaFeature.LaoScreen[];
  context: PoPchaReactContext;
}
