import React, { useContext, useMemo, useState } from 'react';
import { FlatList, ListRenderItemInfo, StyleSheet, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

import { TextBlock } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { ChirpCard, TextInputChirp } from '../components';
import { SocialMediaContext } from '../context';
import { SocialHooks } from '../hooks';
import { SocialFeature } from '../interface';
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
});

const SocialHome = () => {
  const { currentUserPopTokenPublicKey } = useContext(SocialMediaContext);
  const [inputChirp, setInputChirp] = useState('');
  const toast = useToast();
  const laoId = SocialHooks.useCurrentLaoId();

  if (laoId === undefined) {
    throw new Error('Impossible to render Social Home, current lao id is undefined');
  }

  const publishChirp = () => {
    requestAddChirp(currentUserPopTokenPublicKey, inputChirp, laoId)
      .then(() => {
        setInputChirp('');
      })
      .catch((err) => {
        console.error('Failed to post chirp, error:', err);
        toast.show(`Failed to post chirp, error: ${err}`, {
          type: 'danger',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      });
  };

  const chirps = useMemo(() => makeChirpsList(laoId.valueOf()), [laoId]);
  const chirpList = useSelector(chirps);

  const renderChirpState = ({ item }: ListRenderItemInfo<ChirpState>) => (
    <ChirpCard chirp={Chirp.fromState(item)} currentUserPublicKey={currentUserPopTokenPublicKey} />
  );

  return (
    <ScreenWrapper>
      <View style={styles.viewCenter}>
        <View style={styles.homeTextView}>
          <TextBlock text={STRINGS.social_media_navigation_tab_home} />
        </View>
        <View style={styles.userFeed}>
          <TextInputChirp
            testID="new_chirp"
            value={inputChirp}
            onChangeText={setInputChirp}
            onPress={publishChirp}
            // The publish button is disabled when the user public key is not defined
            publishIsDisabledCond={!currentUserPopTokenPublicKey}
            currentUserPublicKey={currentUserPopTokenPublicKey}
          />
          <FlatList
            data={chirpList}
            renderItem={renderChirpState}
            keyExtractor={(item) => item.id.toString()}
          />
        </View>
      </View>
    </ScreenWrapper>
  );
};

export default SocialHome;

export const SocialHomeScreen: SocialFeature.SocialScreen = {
  id: STRINGS.social_media_navigation_tab_home,
  Component: SocialHome,
};
