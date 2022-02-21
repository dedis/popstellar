import { StyleSheet } from 'react-native';

import { Colors } from '../index';

const circularButtonStyles = StyleSheet.create({
  button: {
    justifyContent: 'center',
    alignItems: 'center',
    width: 80,
    height: 80,
    backgroundColor: Colors.blue,
    borderRadius: 80,
  },
});

export default circularButtonStyles;
