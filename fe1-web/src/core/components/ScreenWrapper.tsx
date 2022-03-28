import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, ViewStyle } from 'react-native';
import { ScrollView } from 'react-native-gesture-handler';

const styles = StyleSheet.create({
  view: {
    padding: 16,
  } as ViewStyle,
});

const ScreenWrapper = ({ children }: IPropTypes) => (
  <ScrollView style={styles.view}>{children}</ScrollView>
);

const propTypes = {
  children: PropTypes.node.isRequired,
};

ScreenWrapper.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ScreenWrapper;
