import PropTypes from 'prop-types';
import React, { FunctionComponent, useEffect, useMemo } from 'react';
import { Text, View } from 'react-native';
import { useSelector } from 'react-redux';

import { PoPTextButton } from 'core/components';
import { makeIcon } from 'core/components/PoPIcon';
import { makeMessageSelector } from 'core/network/ingestion';
import { Hash } from 'core/objects';
import { dispatch } from 'core/redux';
import STRINGS from 'resources/strings';

import { WitnessHooks } from '../hooks';
import { WitnessFeature } from '../interface';
import { requestWitnessMessage } from '../network/WitnessMessageApi';
import {
  MessageToWitnessNotification,
  MessageToWitnessNotificationState,
} from '../objects/MessageToWitnessNotification';
import { removeMessageToWitness } from '../reducer';

const WitnessNotification = ({ notification, navigateToNotificationScreen }: IPropTypes) => {
  const messageSelector = useMemo(
    () => makeMessageSelector(notification.messageId),
    [notification.messageId],
  );
  const message = useSelector(messageSelector);

  const discardNotifications = WitnessHooks.useDiscardNotifications();
  const markNotificationAsRead = WitnessHooks.useMarkNotificationAsRead();
  const isEnabled = WitnessHooks.useIsEnabled();
  const laoId = WitnessHooks.useCurrentLaoId();
  const isConnected = WitnessHooks.useConnectedToLao();

  // if the notification state somehow gets out of sync, remove the corresponding notification
  useEffect(() => {
    if (!message) {
      dispatch(discardNotifications({ laoId, notificationIds: [notification.id] }));
      navigateToNotificationScreen();
      console.warn(
        `There was a notification with id ${notification.id} in the redux store referencing a message id (${notification.messageId}) that is not (no longer?) stored`,
      );
    }
  }, [
    laoId,
    navigateToNotificationScreen,
    discardNotifications,
    notification.id,
    notification.messageId,
    message,
  ]);

  const onWitness = () => {
    if (message) {
      dispatch(discardNotifications({ laoId, notificationIds: [notification.id] }));
      if (isEnabled) {
        requestWitnessMessage(message.channel, message.message_id);
      }
      dispatch(removeMessageToWitness(message.message_id));
      navigateToNotificationScreen();
    }
  };

  const onDecline = () => {
    if (message) {
      dispatch(markNotificationAsRead({ laoId, notificationId: notification.id }));
      navigateToNotificationScreen();
    }
  };

  return (
    <View>
      <Text>{JSON.stringify(notification)}</Text>
      <Text>{JSON.stringify(message)}</Text>
      <PoPTextButton onPress={onWitness} disabled={!isConnected}>
        {STRINGS.witness_message_witness}
      </PoPTextButton>
      <PoPTextButton onPress={onDecline} disabled={!isConnected}>
        {STRINGS.meeting_message_decline}
      </PoPTextButton>
    </View>
  );
};

const propTypes = {
  notification: PropTypes.instanceOf(MessageToWitnessNotification).isRequired,
  navigateToNotificationScreen: PropTypes.func.isRequired,
};
WitnessNotification.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default WitnessNotification;

export const WitnessNotificationType = {
  /**
   * Checks whether the given notification is a witness notification
   * @param notification The notification whose type should be checked
   * @returns True if the notification is a witness notification, false otherwise
   */
  isOfType: (
    notification: WitnessFeature.Notification | WitnessFeature.NotificationState,
  ): notification is MessageToWitnessNotification =>
    'type' in notification &&
    notification.type === WitnessFeature.NotificationTypes.MESSAGE_TO_WITNESS,

  fromState: MessageToWitnessNotification.fromState,

  /**
   * Custom cleanup function that removes the message from the witness store
   */
  delete: (notification: WitnessFeature.NotificationState | MessageToWitnessNotificationState) => {
    if (!('messageId' in notification)) {
      throw new Error(
        `MessageToWitnessNotificationState.delete called on notification of type '${notification.type}'`,
      );
    }

    dispatch(removeMessageToWitness(new Hash(notification.messageId)));
  },

  /**
   * The component to render the witness notification
   */
  Component: WitnessNotification as unknown as FunctionComponent<{
    notification: WitnessFeature.Notification;
    navigateToNotificationScreen: () => void;
  }>,

  Icon: makeIcon('witness'),
};
