import PropTypes from 'prop-types';
import React, { FunctionComponent } from 'react';
import { Text } from 'react-native';

import { WitnessFeature, MESSAGE_TO_WITNESS_NOTIFICATION_TYPE } from '../interface';

const WitnessNotification = ({ notification }: IPropTypes) => {
  return <Text>{notification.messageId}</Text>;
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
};
WitnessNotification.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default WitnessNotification;

export const WitnessNotificationTypeComponent = {
  isOfType: (notification: WitnessFeature.Notification) =>
    'type' in notification && notification.type === MESSAGE_TO_WITNESS_NOTIFICATION_TYPE,
  Component: WitnessNotification as unknown as FunctionComponent<{
    notification: WitnessFeature.Notification;
  }>,
};
