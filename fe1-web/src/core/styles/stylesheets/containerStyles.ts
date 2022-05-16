import { StyleSheet } from 'react-native';

import { Spacing } from '../index';

const containerStyles = StyleSheet.create({
  flex: {
    flex: 1,
    justifyContent: 'space-evenly',
  },
  centeredY: {
    flex: 1,
    justifyContent: 'center',
  },
  centeredXY: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  anchoredCenter: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'space-evenly',
  },
  centerWithMargin: {
    flex: 1,
    alignItems: 'center',
    marginHorizontal: Spacing.x5,
    marginVertical: Spacing.x1,
  },
});

export default containerStyles;
