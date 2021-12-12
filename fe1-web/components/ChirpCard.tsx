import React from 'react';
import { View, Text } from 'react-native';
import PropTypes from 'prop-types';
import TimeAgo from 'react-timeago';
import { Ionicons } from '@expo/vector-icons';
import { Chirp } from 'model/objects/Chirp';
import DeleteButton from 'components/DeleteButton';
import styles from 'styles/stylesheets/chirpCard';
import { requestDeleteChirp } from '../network';
import { KeyPairStore } from '../store';

/**
 * Component to display a chirp
 */
const ChirpCard = (props: IPropTypes) => {
  const { chirp } = props;
  const likesText = `  ${chirp.likes}`;

  // This is temporary for now
  const zero = '  0';

  const deleteChirp = () => {
    requestDeleteChirp(chirp.id)
      .catch((err) => {
        console.error('Could not remove chirp, error:', err);
      });
  };

  const isSender = KeyPairStore.getPublicKey().valueOf() === chirp.sender.valueOf();

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
            <Ionicons name="thumbs-up" size={16} color="black" />
            <Text>{likesText}</Text>
          </View>
          <View style={styles.reactionView}>
            <Ionicons name="thumbs-down" size={16} color="black" />
            <Text>{zero}</Text>
          </View>
          <View style={styles.reactionView}>
            <Ionicons name="heart" size={16} color="black" />
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
        { isSender && <DeleteButton action={() => { deleteChirp(); }} />}
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
