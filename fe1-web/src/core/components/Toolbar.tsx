import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { Color, Spacing } from 'core/styles';

import PoPTextButton from './PoPTextButton';

const styles = StyleSheet.create({
  toolbar: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    paddingHorizontal: Spacing.contentSpacing,
    backgroundColor: Color.contrast,
    paddingVertical: Spacing.x1,
    borderColor: Color.separator,
    borderTopWidth: 1,
  } as ViewStyle,
});

/**
 * Tries to follow iOS interface design guidelines
 * https://developer.apple.com/design/human-interface-guidelines/components/menus-and-actions/toolbars
 */
const Toolbar = ({ items }: IPropTypes) => {
  return (
    <View style={styles.toolbar}>
      {items.map((item) => (
        <PoPTextButton
          onPress={item.onPress}
          key={item.id || item.title}
          buttonStyle={item.buttonStyle}
          toolbar>
          {item.title}
        </PoPTextButton>
      ))}
    </View>
  );
};

export const toolbarItemsPropType = PropTypes.arrayOf(
  PropTypes.shape({
    id: PropTypes.string,
    title: PropTypes.string.isRequired,
    onPress: PropTypes.func.isRequired,
    buttonStyle: PropTypes.oneOf<'primary' | 'secondary'>(['primary', 'secondary']),
  }).isRequired,
);

const propTypes = {
  items: toolbarItemsPropType.isRequired,
};

Toolbar.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export type ToolbarItem = IPropTypes['items']['0'];

export default Toolbar;
