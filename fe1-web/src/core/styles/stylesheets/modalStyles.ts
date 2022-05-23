import { StyleSheet, TextStyle, ViewStyle } from 'react-native';

import { white } from '../color';
import { Typography } from '../index';

const modalStyles = StyleSheet.create({
  modalView: {
    backgroundColor: white,
    borderRadius: 10,
    borderWidth: 1,
    margin: 'auto',
    width: 550,
  } as ViewStyle,
  titleView: {
    borderBottomWidth: 1,
  } as ViewStyle,
  modalTitle: {
    ...Typography.importantCentered,
    alignSelf: 'flex-start',
    padding: 20,
    paddingLeft: 10,
  } as TextStyle,
  modalDescription: {
    ...Typography.baseCentered,
    fontSize: 20,
    alignSelf: 'flex-start',
    textAlign: 'left',
    padding: 20,
    paddingLeft: 10,
  } as TextStyle,
  buttonView: {
    alignSelf: 'center',
    flexDirection: 'row',
    paddingBottom: 20,
  } as ViewStyle,
});

export default modalStyles;
