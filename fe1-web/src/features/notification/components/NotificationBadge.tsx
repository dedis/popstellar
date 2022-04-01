import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { useSelector } from 'react-redux';

import containerStyles from 'core/styles/stylesheets/containerStyles';

import { selectUnreadNotificationCount } from '../reducer';

const NotificationBadgeStyles = StyleSheet.create({
  container: {
    ...containerStyles.centeredXY,
    margin: 4,
    width: 18,
    height: 18,
    backgroundColor: '#f00',
    borderRadius: 50,
  } as ViewStyle,
});

const NotificationBadge = () => {
  const count = useSelector(selectUnreadNotificationCount);

  if (count <= 0) {
    return null;
  }

  return <View style={NotificationBadgeStyles.container} />;
};

// The react-navigation Badge parameter does not directly accept components
export default () => <NotificationBadge />;
