import PropTypes from 'prop-types';
import React, { FunctionComponent, useEffect, useMemo } from 'react';
import { Text, View } from 'react-native';
import { Button } from 'react-native-elements';
import { useSelector } from 'react-redux';

import { makeMessageSelector } from 'core/network/ingestion';
import { dispatch } from 'core/redux';

import { WitnessHooks } from '../hooks';
import { WitnessFeature } from '../interface';
import { requestWitnessMessage } from '../network/WitnessMessageApi';
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

  const onWitness = () => {
    if (message) {
      dispatch(
        discardNotifications({ laoId: laoId.valueOf(), notificationIds: [notification.id] }),
      );
      if (isEnabled) {
        requestWitnessMessage(message.channel, message.message_id);
      }
      dispatch(removeMessageToWitness(message.message_id.valueOf()));
      navigateToNotificationScreen();
    }
  };

  const onDecline = () => {
    if (message) {
      dispatch(markNotificationAsRead({ laoId: laoId.valueOf(), notificationId: notification.id }));
      navigateToNotificationScreen();
    }
  };

  // if the notification state somehow gets out of sync, remove the corresponding notification
  useEffect(() => {
    if (!message) {
      dispatch(
        discardNotifications({ laoId: laoId.valueOf(), notificationIds: [notification.id] }),
      );
      navigateToNotificationScreen();
    }
  }, [laoId, navigateToNotificationScreen, discardNotifications, notification.id, message]);

  return (
    <View>
      <Text>{JSON.stringify(notification)}</Text>
      <Text>{JSON.stringify(message)}</Text>
      <Button title="Witness Message" onPress={onWitness} />
      <Button title="Decline Message" onPress={onDecline} />
    </View>
  );
};

const propTypes = {
  notification: PropTypes.shape({
    // WitnessFeature.MessageToWitnessNotification
    id: PropTypes.number.isRequired,
    timestamp: PropTypes.number.isRequired,
    title: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    messageId: PropTypes.string.isRequired,
  }).isRequired,
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
  isOfType: (notification: WitnessFeature.Notification) =>
    'type' in notification &&
    notification.type === WitnessFeature.NotificationTypes.MESSAGE_TO_WITNESS,

  /**
   * Custom cleanup function that removes the message from the witness store
   */
  delete: ((notification: WitnessFeature.MessageToWitnessNotification) => {
    dispatch(removeMessageToWitness(notification.messageId.valueOf()));
  }) as (notification: WitnessFeature.Notification) => void,

  /**
   * The component to render the witness notification
   */
  Component: WitnessNotification as unknown as FunctionComponent<{
    notification: WitnessFeature.Notification;
    navigateToNotificationScreen: () => void;
  }>,
};
