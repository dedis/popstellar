import { StyleSheet, ViewStyle } from 'react-native';

import { Colors } from '../index';
import { base } from '../views';

const circularButtonStyles = StyleSheet.create({
  button: {
    ...base,
    width: 80,
    height: 80,
    backgroundColor: Colors.blue,
    borderRadius: 80,
  } as ViewStyle,
  roundButton: {
    ...base,
    height: 65,
    width: 65,
    shadowColor: Colors.gray,
    shadowOffset: {
      width: 2,
      height: 2,
    },
    borderRadius: 30,
    backgroundColor: Colors.blue,
  } as ViewStyle,
});

export default circularButtonStyles;
