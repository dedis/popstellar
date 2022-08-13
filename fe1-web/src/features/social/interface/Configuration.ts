import { Reducer } from 'redux';

import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash, PopToken } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';
import { PublicKey } from 'core/objects/PublicKey';

import { SOCIAL_REDUCER_PATH, SocialLaoReducerState } from '../reducer';
import { SocialFeature } from './Feature';

export const SOCIAL_FEATURE_IDENTIFIER = 'social';

export interface SocialConfiguration {
  // objects
  messageRegistry: MessageRegistry;

  /* LAO related functions */

  /**
   * Returns the currently active lao. Should be used outside react components
   * @returns The current lao
   */
  getCurrentLao: () => SocialFeature.Lao;

  /**
   * Returns the currently active lao. Should be used inside react components
   * @returns The current lao
   */
  useCurrentLao: () => SocialFeature.Lao;

  /**
   * Returns the currently active lao id. Should be used outside react components
   * @returns The current lao or undefined if there is none.
   */
  getCurrentLaoId: () => Hash | undefined;

  /**
   * Returns the currently active lao id. Should be used inside react components
   * @returns The current lao or undefined if there is none.
   */
  useCurrentLaoId: () => Hash | undefined;

  /* Roll Calls */

  /**
   * Gets the roll call associated to this roll call id
   * To use only in a React component
   * @param rollCallId
   * @returns The roll call or undefined if not found
   */
  useRollCallById: (rollCallId: Hash | string) => SocialFeature.RollCall | undefined;

  /**
   * Gets the list of attendees for a roll call. Should be used inside react components.
   * @param rollCallId - The id of the roll call
   * @returns The list of public keys of the attendees
   */
  useRollCallAttendeesById: (rollCallId: Hash | string) => PublicKey[];

  /* Events */

  /**
   * Deterministically generates a pop token from given lao and rollCall ids
   * @param laoId The lao id to generate a token for
   * @param rollCallId The rollCall id to generate a token for
   * @returns The generated pop token
   */
  generateToken: (laoId: Hash, rollCallId: Hash | undefined) => Promise<PopToken>;
}

export type SocialReactContext = Pick<
  SocialConfiguration,
  /* lao */
  | 'useCurrentLao'
  | 'getCurrentLao'
  | 'useCurrentLaoId'
  | 'getCurrentLaoId'
  /* roll call */
  | 'useRollCallById'
  | 'useRollCallAttendeesById'
  /* wallet */
  | 'generateToken'
>;

export interface SocialInterface extends FeatureInterface {
  socialScreens: SocialFeature.SocialScreen[];
  socialSearchScreens: SocialFeature.SocialSearchScreen[];
  context: SocialReactContext;
  reducers: {
    [SOCIAL_REDUCER_PATH]: Reducer<SocialLaoReducerState>;
  };
}
