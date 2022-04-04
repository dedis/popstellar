import { useNavigation } from '@react-navigation/core';
import PropTypes from 'prop-types';
import React, { FunctionComponent, useEffect, useMemo } from 'react';
import { Text, View } from 'react-native';
import { Button } from 'react-native-elements';
import { useSelector } from 'react-redux';

import { dispatch } from 'core/redux';
import { discardNotification } from 'features/notification/reducer';

import { WitnessFeature, MESSAGE_TO_WITNESS_NOTIFICATION_TYPE } from '../interface';
import { requestWitnessMessage } from '../network/WitnessMessageApi';
import { makeMessageSelector, removeMessageToWitness } from '../reducer';

const WitnessNotification = ({ notification, navigateToNotificationScreen }: IPropTypes) => {
  const messageSelector = useMemo(
    () => makeMessageSelector(notification.messageId),
    [notification.messageId],
  );
  const message = useSelector(messageSelector);

  const onWitness = () => {
    if (message) {
      dispatch(discardNotification(notification.id));
      requestWitnessMessage(message.channel, message.message_id);
      dispatch(removeMessageToWitness(message.message_id.valueOf()));
      navigateToNotificationScreen();
    }
  };

  const onDecline = () => {
    if (message) {
      dispatch(discardNotification(notification.id));
      dispatch(removeMessageToWitness(message.message_id.valueOf()));
      navigateToNotificationScreen();
    }
  };

  // if the notification state somehow gets out of sync, remove the corresponding notification
  useEffect(() => {
    if (!message) {
      dispatch(discardNotification(notification.id));
      navigateToNotificationScreen();
    }
  }, [navigateToNotificationScreen, notification.id, message]);

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

export const WitnessNotificationTypeComponent = {
  isOfType: (notification: WitnessFeature.Notification) =>
    'type' in notification && notification.type === MESSAGE_TO_WITNESS_NOTIFICATION_TYPE,
  Component: WitnessNotification as unknown as FunctionComponent<{
    notification: WitnessFeature.Notification;
    navigateToNotificationScreen: () => void;
  }>,
};
