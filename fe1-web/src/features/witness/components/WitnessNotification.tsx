import PropTypes from 'prop-types';
import React, { FunctionComponent, useEffect, useMemo } from 'react';
import { Text, View, ViewStyle, StyleSheet } from 'react-native';
import { useDispatch, useSelector } from 'react-redux';
import { Dispatch } from 'redux';

import { PoPButton } from 'core/components';
import { makeIcon } from 'core/components/PoPIcon';
import { makeMessageSelector } from 'core/network/ingestion';
import { Hash } from 'core/objects';
import { Color, Typography } from 'core/styles';
import { contrast } from 'core/styles/color';
import STRINGS from 'resources/strings';

import { WitnessHooks } from '../hooks';
import { WitnessFeature } from '../interface';
import { requestWitnessMessage } from '../network/WitnessMessageApi';
import {
  MessageToWitnessNotification,
  MessageToWitnessNotificationState,
} from '../objects/MessageToWitnessNotification';
import { removeMessageToWitness } from '../reducer';

const styles = StyleSheet.create({
  container: {
    padding: 20,
    backgroundColor: Color.contrast,
    borderRadius: 10,
    shadowColor: Color.primary,
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 3,
  } as ViewStyle,
  marginB10: {
    marginBottom: 10,
  },
  marginB15: {
    marginBottom: 15,
  },
  marginT10: {
    marginTop: 10,
  },
  buttonTextStyle: {
    color: contrast,
    textAlign: 'center',
    fontSize: 18,
    margin: 3,
  },
  boldText: {
    fontWeight: 'bold',
  },
});

const WitnessNotification = ({ notification, navigateToNotificationScreen }: IPropTypes) => {
  const messageSelector = useMemo(
    () => makeMessageSelector(notification.messageId),
    [notification.messageId],
  );
  const message = useSelector(messageSelector);
  const decodedData = message && JSON.parse(message.data.decode());

  const dispatch = useDispatch();
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
    dispatch,
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
    <View style={styles.container}>
      <Text style={[Typography.base, Typography.important, styles.marginB10]}>
        {STRINGS.witness_req}
      </Text>
      {decodedData ? (
        <>
          <View style={styles.marginB15}>
            <Text style={[Typography.small, styles.boldText]}>
              {decodedData.object}#{decodedData.action}:
            </Text>
            <Text style={Typography.small}>Name: {decodedData.name}</Text>
            <Text style={Typography.small}>ID: {decodedData.id}</Text>
            <Text style={Typography.small}>
              Created at: {new Date(decodedData.creation * 1000).toLocaleString()}
            </Text>
            <Text style={Typography.small}>
              Proposed start: {new Date(decodedData.proposed_start * 1000).toLocaleString()}
            </Text>
            <Text style={Typography.small}>
              Proposed end: {new Date(decodedData.proposed_end * 1000).toLocaleString()}
            </Text>
            <Text style={Typography.small}>Location: {decodedData.location}</Text>
            {decodedData.description ? (
              <Text style={Typography.small}>Description: {decodedData.description}</Text>
            ) : (
              <Text />
            )}
          </View>

          <View style={styles.marginB15}>
            <Text style={[Typography.small, styles.boldText]}>Message Information:</Text>
            <Text style={Typography.small}>Message ID: {notification.messageId}</Text>
            <Text style={Typography.small}>Received from: {message.receivedFrom}</Text>
            <Text style={Typography.small}>Channel: {message.channel}</Text>
            <Text style={Typography.small}>Sender: {message.sender}</Text>
            <Text style={Typography.small}>Signature: {message.signature}</Text>
            <Text style={Typography.small}>Received at: {message.receivedAt.toDateString()}</Text>
            <Text style={Typography.small}>
              Processed at: {message.processedAt?.toDateString()}
            </Text>
          </View>
        </>
      ) : (
        <Text>No data available.</Text>
      )}
      <View style={[styles.marginB10, styles.marginT10]}>
        <PoPButton
          onPress={onWitness}
          disabled={!isConnected}
          buttonStyle="primary"
          testID="on-witness">
          <Text style={styles.buttonTextStyle}>{STRINGS.witness_message_witness}</Text>
        </PoPButton>
      </View>
      <View style={styles.marginB10}>
        <PoPButton
          onPress={onDecline}
          disabled={!isConnected}
          buttonStyle="primary"
          testID="on-decline">
          <Text style={styles.buttonTextStyle}>{STRINGS.meeting_message_decline}</Text>
        </PoPButton>
      </View>
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
  delete: (
    notification: WitnessFeature.NotificationState | MessageToWitnessNotificationState,
    dispatch: Dispatch,
  ) => {
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
