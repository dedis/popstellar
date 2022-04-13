import { useNavigation } from '@react-navigation/native';
import React, { useEffect, useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { CopiableTextInput, TextBlock, WideButtonView } from 'core/components';
import containerStyles from 'core/styles/stylesheets/containerStyles';
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
const WalletShowSeed = () => {
  /* used to set the mnemonic seed inserted by the user */
  const [seed, setSeed] = useState('');
  const navigation = useNavigation<any>();

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

export default WalletShowSeed;
