import PropTypes from 'prop-types';
import React from 'react';
import { Pressable, StyleProp, TouchableOpacity, View, ViewStyle } from 'react-native';

import { ExtendType } from 'core/types';
/**
 * Wraps a touchable opacity in a pressable component to make karate tests work :)
 */
const PoPTouchableOpacity = React.forwardRef<View, IPropTypes>(
  ({ onPress, containerStyle, style, testID, children }: IPropTypes, ref) => {
    return (
      // <Pressable onPress={onPress} style={containerStyle} ref={ref} testID={testID || undefined}>
      //   {children}
      // </Pressable>
      <TouchableOpacity style={style} onPress={onPress}  ref={ref} testID={testID || undefined}>{children}</TouchableOpacity>
    );
  },
);

//         {<TouchableOpacity style={style} onPress={onPress} testID={testID || undefined}>{children}</TouchableOpacity>}

const propTypes = {
  children: PropTypes.node,
  onPress: PropTypes.func,
  testID: PropTypes.string,

  // we cannot reliably determine the shape of a react native style object
  // eslint-disable-next-line react/forbid-prop-types
  containerStyle: PropTypes.any,
  // eslint-disable-next-line react/forbid-prop-types
  style: PropTypes.any,
};

PoPTouchableOpacity.propTypes = propTypes;

PoPTouchableOpacity.defaultProps = {
  children: undefined,
  onPress: undefined,
  testID: undefined,
  containerStyle: undefined,
  style: undefined,
};

type IPropTypes = ExtendType<
  PropTypes.InferProps<typeof propTypes>,
  {
    containerStyle?: StyleProp<ViewStyle>;
    style?: StyleProp<ViewStyle>;
  }
>;

export default PoPTouchableOpacity;
