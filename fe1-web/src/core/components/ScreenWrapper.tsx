import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, ViewStyle } from 'react-native';
import { ScrollView } from 'react-native-gesture-handler';

import { Spacing } from 'core/styles';

const styles = StyleSheet.create({
  view: {
    padding: Spacing.contentSpacing,
  } as ViewStyle,
});

/**
 * Wraps react components in a screen wrapper that adds a scroll view
 * and thus makes sure that all content can be accessed
 */
const ScreenWrapper = ({ children }: IPropTypes) => (
  <ScrollView style={styles.view}>{children}</ScrollView>
);

const propTypes = {
  children: PropTypes.node.isRequired,
};

ScreenWrapper.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ScreenWrapper;
