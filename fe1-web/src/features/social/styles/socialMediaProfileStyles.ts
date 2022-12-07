import { StyleSheet, ViewStyle } from 'react-native';

import { Border, Color, Spacing } from 'core/styles';

const socialMediaProfileStyles = StyleSheet.create({
  textView: {
    marginTop: Spacing.x1,
  } as ViewStyle,
  userFeed: {
    borderColor: Color.inactive,
    borderTopWidth: Border.width,
    flexDirection: 'column',
    marginTop: Spacing.x1,
  } as ViewStyle,
  textUnavailableView: {
    marginTop: Spacing.x1,
  } as ViewStyle,
});

export default socialMediaProfileStyles;
