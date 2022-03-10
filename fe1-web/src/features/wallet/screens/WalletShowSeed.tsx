import PropTypes from 'prop-types';
import React, { useEffect, useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import containerStyles from 'core/styles/stylesheets/containerStyles';
import { CopiableTextInput, TextBlock, WideButtonView } from 'core/components';
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
 * Wallet screen to obtain a new mnemonic seed
 */
const WalletShowSeed = ({ navigation }: IPropTypes) => {
  /* used to set the mnemonic seed inserted by the user */
  const [seed, setSeed] = useState('');

  useEffect(() => {
    setSeed(Wallet.generateMnemonicSeed());
  }, []);

  return (
    <View style={containerStyles.centeredY}>
      <TextBlock bold text={STRINGS.show_seed_info} />
      <View style={styles.smallPadding} />
      <CopiableTextInput text={seed} />
      <View style={styles.smallPadding} />
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
WalletShowSeed.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default WalletShowSeed;
