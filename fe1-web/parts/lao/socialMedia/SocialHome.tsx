import React, { useState } from 'react';
import {
  FlatList,
  StyleSheet,
  TextStyle,
  View,
  ViewStyle,
} from 'react-native';

import ChirpCard from 'components/ChirpCard';
import TextInputChirp from 'components/TextInputChirp';
import TextBlock from 'components/TextBlock';
import STRINGS from 'res/strings';

import { requestAddChirp } from 'network/MessageApi';
import { Hash, Timestamp } from 'model/objects';

/**
 * UI for the Social Media component
 */
const styles = StyleSheet.create({
  viewCenter: {
    alignSelf: 'center',
    width: 600,
  } as ViewStyle,
  homeTextView: {
    alignSelf: 'flex-start',
    marginTop: 20,
  } as ViewStyle,
  userFeed: {
    flexDirection: 'column',
    marginTop: 20,
  } as ViewStyle,
  textInput: {
    padding: 10,
    borderWidth: 1,
    width: 500,
    alignContent: 'flex-end',
  } as TextStyle,
});

const DATA = [
  {
    id: Hash.fromString('1234'),
    sender: 'Gandalf',
    text: 'You shall not pass! You shall not pass! You shall not pass! You shall not pass! You shall not pass! You shall not pass!',
    time: new Timestamp(1609455600),
    likes: 0,
  },
  {
    id: Hash.fromString('5678'),
    sender: 'Douglas Adams',
    text: 'Don\'t panic.',
    time: new Timestamp(1609455600),
    likes: 100,
  },
];

const SocialHome = () => {
  const [inputChirp, setInputChirp] = useState('');

  const publishChirp = () => {
    requestAddChirp(inputChirp)
      .catch((err) => {
        console.error('Could not add chirp, error:', err);
      });
    setInputChirp('');
  };

  const renderItem = ({ item }) => (
    <ChirpCard
      sender={item.sender}
      text={item.text}
      time={item.time}
      likes={item.likes}
    />
  );

  return (
    <View style={styles.viewCenter}>
      <View style={styles.homeTextView}>
        <TextBlock text={STRINGS.social_media_navigation_tab_home} />
      </View>
      <View style={styles.userFeed}>
        <TextInputChirp
          onChangeText={setInputChirp}
          onPress={publishChirp}
        />
        <FlatList
          data={DATA}
          renderItem={renderItem}
          keyExtractor={(item) => item.id.toString()}
        />
      </View>
    </View>
  );
};

export default SocialHome;
