import { useNavigation } from '@react-navigation/native';
import React, { useEffect, useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { BackRoundButton, CopiableTextInput, TextBlock, WideButtonView } from 'core/components';
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
  container: {
    ...containerStyles.centeredY,
    padding: 20,
    height: '100%',
  } as ViewStyle,
});

/**
 * Wallet screen to obtain a new mnemonic seed
 */
const WalletCreateSeed = () => {
  /* used to set the mnemonic seed inserted by the user */
  const [seed, setSeed] = useState('');

  // FIXME: Navigation should use a defined type here (instead of any)
  const navigation = useNavigation<any>();

  useEffect(() => {
    setSeed(Wallet.generateMnemonicSeed());
  }, []);

  const connectWithSeed = async () => {
    try {
      await Wallet.importMnemonic(seed);
      navigation.reset({
        index: 0,
        routes: [{ name: STRINGS.navigation_wallet_home_tab }],
      });
    } catch {
      navigation.navigate(STRINGS.navigation_wallet_error);
    }
  };

  return (
    <View style={styles.container}>
      <BackRoundButton onClick={() => navigation.navigate(STRINGS.navigation_wallet_setup_tab)} />
      <View style={styles.largePadding} />
      <TextBlock bold text={STRINGS.show_seed_info} />
      <View style={styles.smallPadding} />
      <CopiableTextInput text={seed} />
      <View style={styles.smallPadding} />
      <WideButtonView title={STRINGS.connect_with_this_seed} onPress={() => connectWithSeed()} />
    </View>
  );
};
export default WalletCreateSeed;
