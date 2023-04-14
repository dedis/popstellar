import { Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { PoPchaFeature } from './Features';

export const POPCHA_FEATURE_IDENTIFIER = 'popcha';

export interface PoPchaConfiguration {
  useCurrentLaoId: () => Hash;
}

export type PoPchaReactContext = Pick<PoPchaConfiguration, 'useCurrentLaoId'>;

export interface PoPchaInterface extends FeatureInterface {
  laoScreens: PoPchaFeature.LaoScreen[];
  context: PoPchaReactContext;
}
