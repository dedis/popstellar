import React, { FunctionComponent } from 'react';
import { View } from 'react-native';

import { CopiableTextInput, TextBlock } from 'core/components';
import { KeyPairStore } from 'core/keypair';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';

import { HomeHooks } from '../hooks';

/**
 * Manage the Home screen component: if the user is not connected to any LAO, a welcome message
 * is displayed, otherwise a list available previously connected LAOs is displayed instead
 */
const Home: FunctionComponent = () => {
  const laos = HomeHooks.useLaoList();
  const LaoList = HomeHooks.useLaoListComponent();
  const publicKey = KeyPairStore.getPublicKey();

  return laos && laos.length > 0 ? (
    <>
      <LaoList />
      <TextBlock bold text="Your public key:" />
      <CopiableTextInput text={publicKey.valueOf()} />
    </>
  ) : (
    <View style={containerStyles.centeredY}>
      <TextBlock bold text={STRINGS.home_welcome} />
      <TextBlock bold text={STRINGS.home_connect_lao} />
      <TextBlock bold text={STRINGS.home_launch_lao} />

      <TextBlock bold text="Your public key:" />
      <CopiableTextInput text={publicKey.valueOf()} />
    </View>
  );
};

export default Home;
