import PropTypes from 'prop-types';
import { View, Text } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import React from 'react';
import styles from 'styles/stylesheets/chirpCard';
import TimeAgo from 'react-timeago';
import { Chirp } from '../model/objects/Chirp';
import ChirpCard from './ChirpCard';
import STRINGS from '../res/strings';

// component to display a deleted chirp card
const DeletedChirpCard = (props: IPropTypes) => {
  const { chirp } = props;
  const zero = '  0';

  return (
    <View style={styles.container}>
      <View style={styles.leftView}>
        <Ionicons name="person" size={40} color="black" />
      </View>
      <View style={styles.rightView}>
        <View style={styles.senderView}>
          <Text style={styles.senderText}>{chirp.sender.valueOf()}</Text>
        </View>
        <Text style={styles.deletedChirpText}>{STRINGS.deleted_chirp}</Text>
        <View style={styles.reactionView}>
          <Ionicons name="chatbubbles" size={16} color="black" />
          <Text>{zero}</Text>
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

export default DeletedChirpCard;
