import { AnyAction, Reducer } from 'redux';

import { MessageRegistry } from 'core/network/jsonrpc/messages';
import FeatureInterface from 'core/objects/FeatureInterface';

import { WITNESS_REDUCER_PATH, MessagesToWitnessReducerState } from '../reducer';
import { WitnessFeature } from './Feature';

export const WITNESS_FEATURE_IDENTIFIER = 'witness';

export interface WitnessConfiguration {
  /* objects */

  messageRegistry: MessageRegistry;

  /* lao */

  /**
   * Returns the currently active lao. Should be used outside react components
   * @returns The current lao
   */
  getCurrentLao: () => WitnessFeature.Lao;

  /**
   * Returns whether the user is witness of the current lao
   * @returns Whether the user is witness of the current lao
   */
  isLaoWitness: () => boolean;

  /**
   * Returns the currently active lao. Can only be used inside react components
   * @returns The current lao
   */
  useCurrentLao: () => WitnessFeature.Lao;

  /**
   * Creates an action to add a notification to the redux store
   * @returns A redux action that can be dispatched
   */
  addNotification: (notification: Omit<WitnessFeature.Notification, 'id'>) => AnyAction;

  /**
   * Creates an action that marks a message as read inside the redux store
   * @returns A redux action that can be dispatched
   */
  markNotificationAsRead: (notificationId: number) => AnyAction;

  /**
   * Creates an action to discard a notification
   * @returns A redux action that can be dispatched
   */
  discardNotification: (notificationId: number) => AnyAction;
}

/**
 * The type of the context that is provided to react witness components
 */
export type WintessReactContext = Pick<
  WitnessConfiguration,
  'useCurrentLao' | 'addNotification' | 'markNotificationAsRead' | 'discardNotification'
>;

/**
 * The interface the witness feature exposes
 */
export interface WitnessInterface extends FeatureInterface {
  notificationTypeComponents: {
    isOfType: (notification: WitnessFeature.Notification) => boolean;
    Component: React.ComponentType<{ notification: WitnessFeature.Notification }>;
  }[];

  context: WintessReactContext;

  /* reducers */
  reducers: {
    [WITNESS_REDUCER_PATH]: Reducer<MessagesToWitnessReducerState>;
  };
}
