import React from 'react';
import { StyleSheet, ViewStyle, TextStyle } from 'react-native';

const styles = StyleSheet.create({
  container: {
    marginTop: 50,
    justifyContent: 'center',
    backgroundColor: '#ccc',
  } as ViewStyle,
  textView: {
    padding: 10,
    borderWidth: 1,
    width: 600,
    alignContent: 'flex-end',
  } as TextStyle,
});

const ChirpCard = (props: IPropTypes) => {
  const { sender } = props;
  const { text } = props;
  const { time } = props;
  const { likes } = props;

  return (

  )
}
