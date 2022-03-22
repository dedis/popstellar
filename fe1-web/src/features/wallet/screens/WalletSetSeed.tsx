import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { TextBlock, TextInputLine, WideButtonView } from 'core/components';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import PROPS_TYPE from 'resources/Props';
import STRINGS from 'resources/strings';

import * as Wallet from '../objects';

const styles = StyleSheet.create({
  smallPadding: {
    padding: '1rem',
  } as ViewStyle,
  largePadding: {
    padding: '2rem',
  } as ViewStyle,
});

/**
 * Wallet screen to set an already existing mnemonic
 */
const WalletSetSeed = ({ navigation }: IPropTypes) => {
  /* used to set the mnemonic seed inserted by the user */
  const [seed, setSeed] = useState('');

  const initWallet = async () => {
    try {
      await Wallet.importMnemonic(seed);
      navigation.navigate(STRINGS.navigation_synced_wallet);
    } catch (e) {
      console.error(e);
      navigation.navigate(STRINGS.navigation_wallet_error);
    }
  };

  return (
    <View style={containerStyles.centeredY}>
      <TextBlock text={STRINGS.type_seed_info} />
      <TextInputLine
        placeholder={STRINGS.type_seed_example}
        onChangeText={(input: string) => setSeed(input)}
      />
      <View style={styles.smallPadding} />
      <WideButtonView title={STRINGS.setup_wallet} onPress={() => initWallet()} />
      <WideButtonView
        title={STRINGS.back_to_wallet_home}
        onPress={() => navigation.navigate(STRINGS.navigation_home_tab_wallet)}
      />
    </View>
  );
};

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};
WalletSetSeed.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default WalletSetSeed;
