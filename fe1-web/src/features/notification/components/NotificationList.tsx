import { useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import { ListItem } from '@rneui/themed';
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { View } from 'react-native';
import ReactTimeago from 'react-timeago';

import { NotificationParamList } from 'core/navigation/typing/NotificationParamList';
import { Color, Icon, List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { NotificationHooks } from '../hooks';
import { Notification } from '../objects/Notification';

type NavigationProps = StackScreenProps<
  NotificationParamList,
  typeof STRINGS.navigation_notification_notifications
>;

const NotificationList = ({ title, notifications }: IPropTypes) => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const notificationTypes = NotificationHooks.useNotificationTypes();

  const [showNotifications, setShowNotifications] = useState(true);

  return (
    <ListItem.Accordion
      containerStyle={List.accordionItem}
      content={
        <ListItem.Content>
          <ListItem.Title style={[Typography.base, Typography.important]}>{title}</ListItem.Title>
        </ListItem.Content>
      }
      onPress={() => setShowNotifications(!showNotifications)}
      isExpanded={showNotifications}>
      {notifications.map((notification, idx) => {
        const NotificationType = notificationTypes.find((t) => t.isOfType(notification));

        if (!NotificationType) {
          console.error('Unregistered notification type', notification);
          throw new Error('Unregistered notification type');
        }

        const listStyle = List.getListItemStyles(idx === 0, idx === notifications.length - 1);

        return (
          <ListItem
            key={notification.id}
            containerStyle={listStyle}
            style={listStyle}
            onPress={() =>
              navigation.navigate<'Notification'>(
                STRINGS.navigation_notification_single_notification,
                {
                  notificationId: notification.id,
                },
              )
            }>
            <View style={List.icon}>
              <NotificationType.Icon size={Icon.size} color={Color.primary} />
            </View>
            <ListItem.Content>
              <ListItem.Title style={Typography.base}>{notification.title}</ListItem.Title>
              <ListItem.Subtitle style={Typography.small}>
                <ReactTimeago live date={notification.timestamp * 1000} />
              </ListItem.Subtitle>
            </ListItem.Content>
          </ListItem>
        );
      })}
    </ListItem.Accordion>
  );
};

export default NotificationList;

const propTypes = {
  title: PropTypes.string.isRequired,
  notifications: PropTypes.arrayOf(PropTypes.instanceOf(Notification).isRequired).isRequired,
};
NotificationList.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;
