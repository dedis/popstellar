import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';

import { Color, Spacing, Typography } from 'core/styles';

import PoPTouchableOpacity from './PoPTouchableOpacity';

const styles = StyleSheet.create({
  toolbar: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingHorizontal: Spacing.contentSpacing,
    backgroundColor: Color.contrast,
    paddingVertical: Spacing.x1,
    borderColor: Color.separator,
    borderTopWidth: 1,
  } as ViewStyle,
  toolbarItem: {} as ViewStyle,
});

/**
 * Tries to follow iOS interface design guidelines
 * https://developer.apple.com/design/human-interface-guidelines/components/menus-and-actions/toolbars
 */
const Toolbar = ({ items }: IPropTypes) => {
  return (
    <View style={styles.toolbar}>
      {items.map((item) => (
        <PoPTouchableOpacity onPress={item.onPress} key={item.id || item.title}>
          <View style={styles.toolbarItem}>
            <Text style={[Typography.base, Typography.accent]}>{item.title}</Text>
          </View>
        </PoPTouchableOpacity>
      ))}
    </View>
  );
};

export const toolbarItemsPropType = PropTypes.arrayOf(
  PropTypes.shape({
    id: PropTypes.string,
    title: PropTypes.string.isRequired,
    onPress: PropTypes.func.isRequired,
  }).isRequired,
);

const propTypes = {
  items: toolbarItemsPropType.isRequired,
};

Toolbar.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default Toolbar;
