import React from 'react';
import {
  StyleSheet, ViewStyle, View, TextStyle, Text,
} from 'react-native';
import PropTypes from 'prop-types';
import TimeAgo from 'react-timeago';
import { Ionicons } from '@expo/vector-icons';
import { gray } from 'styles/colors';
import { Chirp } from 'model/objects/Chirp';
import { requestAddReaction } from '../network';

/**
 * Component to display a chirp
 */

const styles = StyleSheet.create({
  container: {
    borderColor: gray,
    borderTopWidth: 0,
    borderWidth: 1,
    flexDirection: 'row',
    padding: 10,
    width: 600,
  } as ViewStyle,
  leftView: {
    width: 60,
  } as ViewStyle,
  rightView: {
    display: 'flex',
    flexDirection: 'column',
  } as ViewStyle,
  senderText: {
    fontSize: 18,
    fontWeight: '600',
  } as TextStyle,
  senderView: {
    fontSize: 18,
    marginTop: 7,
  } as ViewStyle,
  chirpText: {
    fontSize: 18,
    paddingBottom: 20,
    paddingTop: 10,
    width: 520,
  } as TextStyle,
  reactionsView: {
    flexDirection: 'row',
    fontSize: 18,
  } as ViewStyle,
  reactionView: {
    flexDirection: 'row',
    flex: 1,
    marginRight: 10,
  } as ViewStyle,
  timeView: {
    alignSelf: 'flex-end',
    marginTop: 10,
  } as ViewStyle,
});

const ChirpCard = (props: IPropTypes) => {
  const { chirp } = props;
  const likesText = `  ${chirp.likes}`;

  // This is temporary for now
  const zero = '  0';

  const addReaction = (emoji: string) => {
    requestAddReaction(emoji, chirp.id)
      .catch((err) => {
        console.error('Could not add reaction, error: ', err);
      });
  };

  return (
    <View style={styles.container}>
      <View style={styles.leftView}>
        <Ionicons name="person" size={40} color="black" />
      </View>
      <View style={styles.rightView}>
        <View style={styles.senderView}>
          <Text style={styles.senderText}>{chirp.sender.valueOf()}</Text>
        </View>
        <Text style={styles.chirpText}>{chirp.text}</Text>
        <View style={styles.reactionsView}>
          <View style={styles.reactionView}>
            <Ionicons.Button
              name="thumbs-up"
              size={16}
              color="black"
              onPress={() => addReaction('👍')}
            />
            <Text>{likesText}</Text>
          </View>
          <View style={styles.reactionView}>
            <Ionicons.Button
              name="thumbs-down"
              size={16}
              color="black"
              onPress={() => addReaction('👎')}
            />
            <Text>{zero}</Text>
          </View>
          <View style={styles.reactionView}>
            <Ionicons.Button
              name="heart"
              size={16}
              color="black"
              onPress={() => addReaction('❤️')}
            />
            <Text>{zero}</Text>
          </View>
          <View style={styles.reactionView}>
            <Ionicons name="chatbubbles" size={16} color="black" />
            <Text>{zero}</Text>
          </View>
        </View>
        <View style={styles.timeView}>
          <TimeAgo date={chirp.time.valueOf() * 1000} />
        </View>
      </View>
    </View>
  );
};

const propTypes = {
  chirp: PropTypes.instanceOf(Chirp).isRequired,
};

ChirpCard.prototype = propTypes;

type IPropTypes = {
  chirp: Chirp,
};

export default ChirpCard;
