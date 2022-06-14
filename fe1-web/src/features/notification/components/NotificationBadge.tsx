import React, { useMemo } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { useSelector } from 'react-redux';

import { Color } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';

import { NotificationHooks } from '../hooks';
import { makeUnreadNotificationCountSelector } from '../reducer';

const NotificationBadgeStyles = StyleSheet.create({
  container: {
    ...containerStyles.centeredXY,
    margin: 4,
    width: 18,
    height: 18,
    backgroundColor: Color.red,
    borderRadius: 50,
  } as ViewStyle,
});

const NotificationBadge = () => {
  const laoId = NotificationHooks.useCurrentLaoId();
  const selectUnreadNotificationCount = useMemo(
    () => makeUnreadNotificationCountSelector(laoId.valueOf()),
    [laoId],
  );
  const count = useSelector(selectUnreadNotificationCount);

  if (count <= 0) {
    return null;
  }

  return <View style={NotificationBadgeStyles.container} />;
};

// The react-navigation Badge parameter does not directly accept components
export default () => <NotificationBadge />;
