import React from 'react';
import {
  StyleSheet, ViewStyle, View, TextStyle, Text,
} from 'react-native';
import PropTypes from 'prop-types';
import { Timestamp } from '../model/objects';
import TextBlock from './TextBlock';

const styles = StyleSheet.create({
  container: {
    marginTop: 50,
    justifyContent: 'center',
    flexDirection: 'column',
    display: 'flex',
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
    <View style={styles.container}>
      <Text>{sender}</Text>
      <View style={styles.textView}>
        <TextBlock text={text} />
      </View>
      <Text style={styles.textView}>{time.timestampToDate().toString()}</Text>
    </View>
  );
};

const propTypes = {
  sender: PropTypes.string.isRequired,
  text: PropTypes.string.isRequired,
  time: PropTypes.instanceOf(Date).isRequired,
  likes: PropTypes.number.isRequired,
};

ChirpCard.prototype = propTypes;

type IPropTypes = {
  sender: string,
  text: string,
  time: Timestamp,
  // like: number,
};

export default ChirpCard;
