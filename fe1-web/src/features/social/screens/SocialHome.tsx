import React, { useContext, useMemo, useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { List, Spacing } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';

import { ChirpCard, TextInputChirp } from '../components';
import { SocialMediaContext } from '../context';
import { SocialHooks } from '../hooks';
import { requestAddChirp } from '../network/SocialMessageApi';
import { makeChirpsList } from '../reducer';

/**
 * UI for the Social Media home screen component
 */

const styles = StyleSheet.create({
  userFeed: {
    flexDirection: 'column',
  } as ViewStyle,
  chirpList: {
    marginTop: Spacing.x1,
  } as ViewStyle,
});

const SocialHome = () => {
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
    // button is disabled if publicKey is not set
    if (!currentUserPopTokenPublicKey) {
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
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      });
  };

  const chirps = useMemo(() => makeChirpsList(laoId), [laoId]);
  const chirpList = useSelector(chirps);

  return (
    <ScreenWrapper>
      <View style={styles.userFeed}>
        <TextInputChirp
          testID="new_chirp"
          value={inputChirp}
          onChangeText={setInputChirp}
          onPress={publishChirp}
          disabled={publishDisabled}
          currentUserPublicKey={currentUserPopTokenPublicKey}
        />
        <View style={[List.container, styles.chirpList]}>
          {chirpList.map((chirp, i) => (
            <ChirpCard
              key={chirp.id.toString()}
              chirp={chirp}
              isFirstItem={i === 0}
              isLastItem={i === chirpList.length - 1}
            />
          ))}
        </View>
      </View>
    </ScreenWrapper>
  );
};

export default SocialHome;
