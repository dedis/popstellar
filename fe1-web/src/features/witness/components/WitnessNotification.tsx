import PropTypes from 'prop-types';
import React, { FunctionComponent, useEffect, useMemo } from 'react';
import { Text, View, ViewStyle, StyleSheet } from 'react-native';
import { useSelector } from 'react-redux';

import { PoPButton, PoPTextButton } from 'core/components';
import { makeIcon } from 'core/components/PoPIcon';
import { makeMessageSelector } from 'core/network/ingestion';
import { Hash, Timestamp } from 'core/objects';
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
import { Spacing, Typography } from 'core/styles';
import ReactTimeago from 'react-timeago';
import { contrast } from 'core/styles/color';




const styles = StyleSheet.create({
  buttonContainer: {
    margin: '10px 0px',
    padding: '10px 0px',
  } as ViewStyle,
  container: {
    flex: 1,
    justifyContent: 'space-between',
    marginVertical: Spacing.contentSpacing,
  } as ViewStyle,
});



const WitnessNotification = ({ notification, navigateToNotificationScreen }: IPropTypes) => {
  const messageSelector = useMemo(
    () => makeMessageSelector(notification.messageId),
    [notification.messageId],
  );
  const message = useSelector(messageSelector);
  const decodedData = message == undefined ? undefined : JSON.parse(message.data.decode());

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
    <View style={{ padding: 20, backgroundColor: '#fff', borderRadius: 10, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 2, elevation: 3 }}>
    <Text style={[Typography.base, Typography.important, { marginBottom: 10 }]}>
      {STRINGS.witnessing_req}
    </Text>
      {decodedData && message ? (
        <>
    <View style={{ marginBottom: 15 }}>
      <Text style={[Typography.small, { fontWeight: 'bold' }]}>{decodedData.object}#{decodedData.action}:</Text>
      <Text style={Typography.small}>Name: {decodedData.name}</Text>
      <Text style={Typography.small}>ID: {decodedData.id}</Text>
      <Text style={Typography.small}>Created at: {new Date(decodedData.creation * 1000).toLocaleString()}</Text>
      <Text style={Typography.small}>Proposed start: {new Date(decodedData.proposed_start * 1000).toLocaleString()}</Text>
      <Text style={Typography.small}>Proposed end: {new Date(decodedData.proposed_end * 1000).toLocaleString()}</Text>
      <Text style={Typography.small}>Location: {decodedData.location}</Text>
    </View>

    <View style={{ marginBottom: 15 }}>
      <Text style={[Typography.small, { fontWeight: 'bold' }]}>Message Information:</Text>
      <Text style={Typography.small}>Message ID: {notification.messageId}</Text>
      <Text style={Typography.small}>Received from: {message.receivedFrom}</Text>
      <Text style={Typography.small}>Channel: {message.channel}</Text>
      <Text style={Typography.small}>Sender: {message.sender}</Text>
      <Text style={Typography.small}>Signature: {message.signature}</Text>
      <Text style={Typography.small}>Received at: {message.receivedAt.toDateString()}</Text>
      <Text style={Typography.small}>Processed at: {message.processedAt?.toDateString()}</Text>
    </View>
        </>
      ) : (
        <Text>
          No data available.
        </Text>
      )}
      <View style={{ marginBottom: 10, marginTop: 10 }}>
          <PoPButton
          onPress={onWitness} disabled={!isConnected}
          buttonStyle={'primary'}>
            <Text style={{ color: contrast, textAlign: 'center', fontSize: 18, margin: 3}}>{STRINGS.witness_message_witness}</Text>
          </PoPButton>
        </View>
        <View style={{ marginBottom: 10 }}>
          <PoPButton
          onPress={onDecline} disabled={!isConnected}
          buttonStyle={'primary'}>
            <Text style={{ color: contrast, textAlign: 'center', fontSize: 18, margin: 3}}>{STRINGS.meeting_message_decline}</Text>
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
