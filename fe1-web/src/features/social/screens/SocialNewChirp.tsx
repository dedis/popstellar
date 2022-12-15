import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import * as React from 'react';
import { useContext, useState } from 'react';
import { StyleSheet, Text, TextStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { SocialHomeParamList } from 'core/navigation/typing/SocialHomeParamList';
import { SocialParamList } from 'core/navigation/typing/SocialParamList';
import { Spacing, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { TextInputChirp } from '../components';
import { SocialMediaContext } from '../context';
import { SocialHooks } from '../hooks';
import { requestAddChirp } from '../network/SocialMessageApi';

type NavigationProps = CompositeScreenProps<
  CompositeScreenProps<
    StackScreenProps<SocialHomeParamList, typeof STRINGS.social_media_home_navigation_new_chirp>,
    StackScreenProps<SocialParamList, typeof STRINGS.social_media_navigation_tab_home>
  >,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_social_media>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

const styles = StyleSheet.create({
  errorMessage: {
    marginTop: Spacing.x1,
  } as TextStyle,
});

const SocialNewChirp = () => {
  const { currentUserPopTokenPublicKey } = useContext(SocialMediaContext);
  const [inputChirp, setInputChirp] = useState('');
  const toast = useToast();
  const laoId = SocialHooks.useCurrentLaoId();
  const isConnected = SocialHooks.useConnectedToLao();
  const navigation = useNavigation<NavigationProps['navigation']>();

  if (laoId === undefined) {
    throw new Error('Impossible to render Social Home, current lao id is undefined');
  }

  // The publish button is disabled in offline mode and when the user public key is not defined
  const publishDisabled = !isConnected || !currentUserPopTokenPublicKey;

  const publishChirp = () => {
    if (publishDisabled) {
      return;
    }

    requestAddChirp(currentUserPopTokenPublicKey, inputChirp, laoId)
      .then(() => {
        setInputChirp('');
        navigation.goBack();
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

  return (
    <ScreenWrapper>
      <TextInputChirp
        testID="new_chirp"
        value={inputChirp}
        onChangeText={setInputChirp}
        onPress={publishChirp}
        disabled={publishDisabled}
        currentUserPublicKey={currentUserPopTokenPublicKey}
      />
      {!currentUserPopTokenPublicKey && (
        <Text style={[Typography.base, Typography.error, styles.errorMessage]}>
          {STRINGS.social_media_create_chirp_no_pop_token}
        </Text>
      )}
    </ScreenWrapper>
  );
};

export default SocialNewChirp;
