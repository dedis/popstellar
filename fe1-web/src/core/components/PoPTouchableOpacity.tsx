import PropTypes from 'prop-types';
import React from 'react';
import { Pressable, StyleProp, View, ViewStyle } from 'react-native';
import { TouchableOpacity } from 'react-native-gesture-handler';

import { ExtendType } from 'core/types';

/**
 * Wraps a touchable opacity in a pressable component to make karate tests work :)
 */
const PoPTouchableOpacity = ({
  onPress,
  containerStyle,
  style,
  testID,
  ref,
  children,
}: IPropTypes) => {
  return (
    <Pressable onPress={onPress} style={containerStyle} ref={ref} testID={testID || undefined}>
      <TouchableOpacity containerStyle={containerStyle} style={style}>
        {children}
      </TouchableOpacity>
    </Pressable>
  );
};

const propTypes = {
  children: PropTypes.node.isRequired,
  onPress: PropTypes.func,
  testID: PropTypes.string,

  // we cannot reliably determine the shape of a react native style object
  // eslint-disable-next-line react/forbid-prop-types
  containerStyle: PropTypes.any,
  // eslint-disable-next-line react/forbid-prop-types
  style: PropTypes.any,

  // ref types are opaque to us
  // eslint-disable-next-line react/forbid-prop-types
  ref: PropTypes.any,
};

PoPTouchableOpacity.propTypes = propTypes;

PoPTouchableOpacity.defaultProps = {
  onPress: undefined,
  testID: undefined,
  containerStyle: undefined,
  style: undefined,
  ref: undefined,
};

type IPropTypes = ExtendType<
  PropTypes.InferProps<typeof propTypes>,
  {
    containerStyle?: StyleProp<ViewStyle>;
    style?: StyleProp<ViewStyle>;
    ref?: React.Ref<View>;
  }
>;

export default PoPTouchableOpacity;
