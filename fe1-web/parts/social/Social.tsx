import React, { useState } from 'react';
import {
  FlatList,
  ScrollView,
  StyleSheet,
  TextStyle,
  View,
  ViewStyle,
} from 'react-native';

import TextBlock from 'components/TextBlock';
import TextInputChirp from 'components/TextInputChirp';
import WideButtonView from 'components/WideButtonView';
import STRINGS from 'res/strings';

import { requestAddChirp } from 'network/MessageApi';
import { OpenedLaoStore, SocialStore } from 'store';
import { Chirp } from 'model/objects/Chirp';
import { subscribeToChannel } from '../../network/CommunicationApi';
import { generalChirpsChannel, Lao } from '../../model/objects';

/**
 * UI for the Social Media component
 */
const styles = StyleSheet.create({
  view: {
    alignItems: 'center',
  } as ViewStyle,
  textInput: {
    padding: 10,
    borderWidth: 1,
    width: 500,
    alignContent: 'flex-end',
  } as TextStyle,
});

const Social = () => {
  const [inputChirp, setInputChirp] = useState('');

  const publishChirp = () => {
    requestAddChirp(inputChirp)
      .catch((err) => {
        console.error('Could not add chirp, error:', err);
      });
  };

  let chirpList = SocialStore.getAllChirps();

  const updateChirps = () => {
    chirpList = SocialStore.getAllChirps();
  };

  const subscribeToSocialChannel = () => {
    const currentLao: Lao = OpenedLaoStore.get();
    const socialChannel = generalChirpsChannel(currentLao.id);
    subscribeToChannel(socialChannel).then(() => {}).catch((err) => {
      console.error('Could not subscribe to Social Media general channel, error: ', err);
    });
  };

  const renderChirp = (chirp: Chirp) => (
    <TextBlock text={chirp.text} />
  );

  return (
    <View style={styles.view}>
      <ScrollView>
        <TextInputChirp
          onChangeText={setInputChirp}
          onPress={publishChirp}
        />
        <WideButtonView
          title="Subscribe"
          onPress={subscribeToSocialChannel}
        />
        <WideButtonView
          title="Update"
          onPress={updateChirps}
        />
        <TextBlock text={STRINGS.feed_description} />
        <FlatList
          data={chirpList}
          renderItem={renderChirp}
          keyExtractor={chirp => chirp.time}
        />
      </ScrollView>
    </View>
  );
};

export default Social;
