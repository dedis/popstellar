import React, { useEffect, useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import PropTypes from 'prop-types';

import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';
import { CopiableTextInput, TextBlock, WideButtonView } from 'core/components';
import PROPS_TYPE from 'resources/Props';

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
 * Wallet screen to obtain a new mnemonic seed
 */
const WalletCreateSeed = ({ navigation }: IPropTypes) => {
  /* used to set the mnemonic seed inserted by the user */
  const [seed, setSeed] = useState('');

  useEffect(() => {
    setSeed(Wallet.generateMnemonicSeed());
  }, []);

  const connectWithSeed = async () => {
    try {
      await Wallet.importMnemonic(seed);
      navigation.navigate(STRINGS.navigation_wallet_home_tab);
    } catch {
      navigation.navigate(STRINGS.navigation_wallet_error);
    }
  };

  return (
    <View style={containerStyles.centered}>
      <TextBlock bold text={STRINGS.show_seed_info} />
      <View style={styles.smallPadding} />
      <CopiableTextInput text={seed} />
      <View style={styles.smallPadding} />
      <WideButtonView title={STRINGS.connect_with_this_seed} onPress={() => connectWithSeed()} />
      <WideButtonView
        title={STRINGS.back_to_wallet_setup}
        onPress={() => navigation.navigate(STRINGS.navigation_wallet_setup_tab)}
      />
    </View>
  );
};

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};
WalletCreateSeed.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default WalletCreateSeed;
