import React from 'react';
import {
  StyleSheet, ViewStyle, View, TextStyle, Text,
} from 'react-native';
import PropTypes from 'prop-types';
import TimeAgo from 'react-timeago';
import { Ionicons } from '@expo/vector-icons';
import { Timestamp } from '../model/objects';
import TextBlock from './TextBlock';

const styles = StyleSheet.create({
  container: {
    marginTop: 50,
    justifyContent: 'center',
    borderWidth: 2,
    flexDirection: 'column',
    display: 'flex',
  } as ViewStyle,
  textView: {
    padding: 10,
    borderWidth: 1,
    width: 600,
    alignContent: 'flex-end',
  } as TextStyle,
  senderView: {
    fontSize: 20,
    fontWeight: '600',
    marginTop: 7,
  } as TextStyle,
});

const ChirpCard = (props: IPropTypes) => {
  const { sender } = props;
  const { text } = props;
  const { time } = props;
  const { likes } = props;

  return (
    <View style={styles.container}>
      <Text style={styles.senderView}>{sender}</Text>
      <View style={styles.textView}>
        <TextBlock text={text} />
      </View>
      <View style={{ flexDirection: 'row' }}>
        <View style={{ flex: 1 }}>
          <Ionicons name="thumbs-up" size={16} color="black" />
        </View>
        <View style={{ flex: 3 }}>
          <Text>{likes}</Text>
        </View>
        <View style={{ flex: 6 }}>
          <TimeAgo date={time.valueOf() * 1000} />
        </View>
      </View>
    </View>
  );
};

const propTypes = {
  sender: PropTypes.string.isRequired,
  text: PropTypes.string.isRequired,
  time: PropTypes.instanceOf(Timestamp).isRequired,
  likes: PropTypes.number.isRequired,
};

ChirpCard.prototype = propTypes;

type IPropTypes = {
  sender: string,
  text: string,
  time: Timestamp,
  likes: number,
};

export default ChirpCard;
