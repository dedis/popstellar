import { StyleSheet, ViewStyle } from 'react-native';

import { Views } from 'core/styles';

export const createEventStyles = StyleSheet.create({
  view: {
    ...Views.base,
    flexDirection: 'row',
    zIndex: 3,
  } as ViewStyle,
  viewVertical: {
    ...Views.base,
    flexDirection: 'column',
    zIndex: 3,
  } as ViewStyle,
  padding: { padding: 5 } as ViewStyle,
  zIndexInitial: { zIndex: 0 } as ViewStyle,
});
