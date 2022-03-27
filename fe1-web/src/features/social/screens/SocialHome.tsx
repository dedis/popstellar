import PropTypes from 'prop-types';
import React, { useMemo, useState } from 'react';
import { FlatList, ListRenderItemInfo, StyleSheet, TextStyle, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

import { TextBlock } from 'core/components';
import { PublicKey } from 'core/objects';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { ChirpCard, TextInputChirp } from '../components';
import { requestAddChirp } from '../network/SocialMessageApi';
import { Chirp, ChirpState } from '../objects';
import { makeChirpsList } from '../reducer';

/**
 * UI for the Social Media home screen component
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

const SocialHome = (props: IPropTypes) => {
  const { currentUserPublicKey } = props;
  const [inputChirp, setInputChirp] = useState('');
  const toast = useToast();

  const publishChirp = () => {
    requestAddChirp(currentUserPublicKey, inputChirp).catch((err) => {
      console.error('Failed to post chirp, error:', err);
      toast.show(`Failed to post chirp, error: ${err}`, {
        type: 'danger',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    });
  };

  const chirps = useMemo(makeChirpsList, []);
  const chirpList = useSelector(chirps);

  const renderChirpState = ({ item }: ListRenderItemInfo<ChirpState>) => (
    <ChirpCard chirp={Chirp.fromState(item)} currentUserPublicKey={currentUserPublicKey} />
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
          // The publish button is disabled when the user public key is not defined
          publishIsDisabledCond={currentUserPublicKey.valueOf() === ''}
          currentUserPublicKey={currentUserPublicKey}
        />
        <FlatList
          data={chirpList}
          renderItem={renderChirpState}
          keyExtractor={(item) => item.id.toString()}
        />
      </View>
    </View>
  );
};

const propTypes = {
  currentUserPublicKey: PropTypes.instanceOf(PublicKey).isRequired,
};

SocialHome.prototype = propTypes;

type IPropTypes = {
  currentUserPublicKey: PublicKey;
};

export default SocialHome;
