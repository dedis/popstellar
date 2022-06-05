import { useNavigation } from '@react-navigation/native';
import React, { useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { BackRoundButton, TextBlock, TextInputLine, WideButtonView } from 'core/components';
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
 * Wallet screen to set an already existing mnemonic
 */
const WalletSetSeed = () => {
  /* used to set the mnemonic seed inserted by the user */
  const [seed, setSeed] = useState('');

  // FIXME: Navigation should use a defined type here (instead of any)
  const navigation = useNavigation<any>();

  const initWallet = async () => {
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
      <TextBlock text={STRINGS.type_seed_info} />
      <TextInputLine
        placeholder={STRINGS.type_seed_example}
        onChangeText={(input: string) => setSeed(input)}
      />
      <View style={styles.smallPadding} />
      <WideButtonView title={STRINGS.save_seed_and_connect} onPress={() => initWallet()} />
    </View>
  );
};

export default WalletSetSeed;
