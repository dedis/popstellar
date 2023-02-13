import * as React from 'react';
import { useContext, useState } from 'react';
import { StyleSheet, Text, TextStyle, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import { Spacing, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { SocialMediaContext } from '../context';
import { SocialHooks } from '../hooks';
import { requestAddChirp } from '../network/SocialMessageApi';
import { TextInputChirp } from './index';

const styles = StyleSheet.create({
  wrapper: {
    marginBottom: Spacing.x1,
  } as ViewStyle,
  errorMessage: {
    marginTop: Spacing.x1,
  } as TextStyle,
});

const NewChirp = () => {
  const { currentUserPopTokenPublicKey } = useContext(SocialMediaContext);
  const [inputChirp, setInputChirp] = useState('');
  const toast = useToast();
  const laoId = SocialHooks.useCurrentLaoId();
  const isConnected = SocialHooks.useConnectedToLao();

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
      })
      .catch((err) => {
        console.error('Failed to post chirp, error:', err);
        toast.show(`Failed to post chirp, error: ${err}`, {
          type: 'danger',
          placement: 'bottom',
          duration: FOUR_SECONDS,
        });
      });
  };

  return (
    <View style={styles.wrapper}>
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
    </View>
  );
};

export default NewChirp;
