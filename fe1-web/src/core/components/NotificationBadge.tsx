import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import containerStyles from 'core/styles/stylesheets/containerStyles';

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

const NotificationBadge = ({ children: count }: IPropTypes) => {
  if (count <= 0) {
    return null;
  }

  return <View style={NotificationBadgeStyles.container} />;
};

const propTypes = {
  children: PropTypes.number.isRequired,
};
NotificationBadge.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default NotificationBadge;
