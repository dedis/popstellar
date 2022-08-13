import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo, useState } from 'react';
import { FlatList, ListRenderItemInfo, StyleSheet, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

import { TextBlock } from 'core/components';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { SocialParamList } from 'core/navigation/typing/SocialParamList';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { ChirpCard, TextInputChirp } from '../components';
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

type NavigationProps = CompositeScreenProps<
  StackScreenProps<SocialParamList, typeof STRINGS.social_media_navigation_tab_home>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_social_media>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

const SocialHome = () => {
  const route = useRoute<NavigationProps['route']>();
  const { currentUserPublicKey } = route.params;
  const [inputChirp, setInputChirp] = useState('');
  const toast = useToast();
  const laoId = SocialHooks.useCurrentLaoId();

  if (laoId === undefined) {
    throw new Error('Impossible to render Social Home, current lao id is undefined');
  }

  const publishChirp = () => {
    requestAddChirp(currentUserPublicKey, inputChirp, laoId)
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
          testID="new_chirp"
          value={inputChirp}
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

export default SocialHome;

export const SocialHomeScreen: SocialFeature.SocialScreen = {
  id: STRINGS.social_media_navigation_tab_home,
  Component: SocialHome,
};
