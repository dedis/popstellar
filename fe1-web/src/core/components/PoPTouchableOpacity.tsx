import PropTypes from 'prop-types';
import React from 'react';
import { StyleProp, TouchableOpacity, ViewStyle } from 'react-native';

import { ExtendType } from 'core/types';
/**
 * Wraps a touchable opacity in a pressable component to make karate tests work :)
 * /!\ no longer wraps due to UI bugs (dayan9265, 06-06-2023)
 */
const PoPTouchableOpacity = React.forwardRef<TouchableOpacity, IPropTypes>(
  ({ onPress, style, testID, children }: IPropTypes, ref) => {
    return (
      <TouchableOpacity
        style={style}
        onPress={onPress !== null ? onPress : undefined}
        ref={ref}
        testID={testID || undefined}>
        {children}
      </TouchableOpacity>
    );
  },
);

const propTypes = {
  children: PropTypes.node,
  onPress: PropTypes.func,
  testID: PropTypes.string,

  // we cannot reliably determine the shape of a react native style object
  // eslint-disable-next-line react/forbid-prop-types
  style: PropTypes.any,
};

PoPTouchableOpacity.propTypes = propTypes;

PoPTouchableOpacity.defaultProps = {
  children: undefined,
  onPress: undefined,
  testID: undefined,
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
