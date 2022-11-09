import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { ScrollView } from 'react-native-gesture-handler';

import { Spacing } from 'core/styles';

import Toolbar, { toolbarItemsPropType } from './Toolbar';

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  view: {
    padding: Spacing.contentSpacing,
  } as ViewStyle,
});

/**
 * Wraps react components in a screen wrapper that adds a scroll view
 * and thus makes sure that all content can be accessed
 */
const ScreenWrapper = ({ children, toolbarItems }: IPropTypes) => (
  <View style={styles.container}>
    <ScrollView style={styles.view}>{children}</ScrollView>
    {toolbarItems && <Toolbar items={toolbarItems} />}
  </View>
);

const propTypes = {
  children: PropTypes.node.isRequired,
  toolbarItems: toolbarItemsPropType,
};

ScreenWrapper.propTypes = propTypes;
ScreenWrapper.defaultProps = {
  toolbarItems: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ScreenWrapper;
