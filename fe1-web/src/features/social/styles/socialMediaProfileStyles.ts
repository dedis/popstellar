import { StyleSheet, TextStyle, ViewStyle } from 'react-native';

import { gray } from 'core/styles/color';

const socialMediaProfileStyles = StyleSheet.create({
  viewCenter: {
    alignSelf: 'center',
    width: 600,
  } as ViewStyle,
  topView: {
    marginTop: 20,
    flexDirection: 'column',
    alignSelf: 'flex-start',
  } as ViewStyle,
  textView: {
    alignSelf: 'flex-start',
    marginTop: 15,
  } as ViewStyle,
  profileText: {
    marginBottom: 5,
    fontSize: 22,
    fontWeight: 'bold',
  } as TextStyle,
  userFeed: {
    borderColor: gray,
    borderTopWidth: 1,
    flexDirection: 'column',
    marginTop: 20,
  } as ViewStyle,
  textUnavailableView: {
    alignSelf: 'center',
    width: 600,
    marginTop: 20,
  } as ViewStyle,
});

export default socialMediaProfileStyles;
