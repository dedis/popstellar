import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';

import { Color, Spacing, Typography } from 'core/styles';

const styles = StyleSheet.create({
  toolbar: {
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
        <View style={styles.toolbarItem} key={item.id || item.title}>
          <Text style={Typography.base}>{item.title}</Text>
        </View>
      ))}
    </View>
  );
};

export const toolbarItemsPropType = PropTypes.arrayOf(
  PropTypes.shape({
    id: PropTypes.string,
    title: PropTypes.string.isRequired,
  }).isRequired,
);

const propTypes = {
  items: toolbarItemsPropType.isRequired,
};

Toolbar.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default Toolbar;
