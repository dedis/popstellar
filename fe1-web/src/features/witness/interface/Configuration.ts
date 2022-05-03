import { AnyAction, Reducer } from 'redux';

import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { WITNESS_REDUCER_PATH, MessagesToWitnessReducerState } from '../reducer';
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
   * A hook returning the current lao id
   * @returns The current lao id
   */
  useCurrentLaoId: () => Hash | undefined;

  /**
   * Returns whether the user is witness of the current lao
   */
  isLaoWitness: () => boolean;

  /**
   * Creates an action to add a notification to the redux store
   * @returns A redux action that can be dispatched
   */
  addNotification: (notification: Omit<WitnessFeature.Notification, 'id'>) => AnyAction;

  /**
   * Creates an action that marks a message as read inside the redux store
   * @returns A redux action that can be dispatched
   */
  markNotificationAsRead: (args: { laoId: string; notificationId: number }) => AnyAction;

  /**
   * Creates an action to discard a notification
   * @returns A redux action that can be dispatched
   */
  discardNotifications: (args: { laoId: string; notificationIds: number[] }) => AnyAction;
}

/**
 * The type of the context that is provided to react witness components
 */
export type WitnessReactContext = Pick<
  WitnessConfiguration,
  | 'enabled'
  | 'useCurrentLaoId'
  | 'addNotification'
  | 'markNotificationAsRead'
  | 'discardNotifications'
>;

/**
 * The interface the witness feature exposes
 */
export interface WitnessInterface extends FeatureInterface {
  notificationTypes: {
    isOfType: (notification: WitnessFeature.Notification) => boolean;

    delete?: (notification: WitnessFeature.Notification) => void;

    Component: React.ComponentType<{
      notification: WitnessFeature.Notification;
      navigateToNotificationScreen: () => void;
    }>;
  }[];

  context: WintessReactContext;

  /* reducers */
  reducers: {
    [WITNESS_REDUCER_PATH]: Reducer<MessagesToWitnessReducerState>;
  };
}
