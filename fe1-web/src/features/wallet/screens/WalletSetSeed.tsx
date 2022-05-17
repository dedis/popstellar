import { useNavigation } from '@react-navigation/native';
import React, { useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';

import { TextBlock, TextInputLine, Button } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppScreen } from 'core/navigation/AppNavigation';
import { Colors, Typography } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';

import * as Wallet from '../objects';

const styles = StyleSheet.create({
  welcomeView: {
    flex: 1,
    backgroundColor: Colors.accent,
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
      navigation.navigate(STRINGS.app_navigation_tab_home, {
        screen: STRINGS.navigation_wallet_home_tab,
      });
    } catch {
      navigation.navigate(STRINGS.navigation_wallet_error);
    }
  };

  return (
    <View style={styles.welcomeView}>
      <ScreenWrapper>
        <View>
          <Text style={[Typography.heading, Typography.negative]}>{STRINGS.wallet_seed_info}</Text>
          <TextInputLine
            placeholder={STRINGS.type_seed_example}
            onChangeText={(input: string) => setSeed(input)}
            negative
          />
          <Button onPress={() => initWallet()} negative>
            <Text style={[Typography.base, Typography.centered, Typography.negative]}>
              {STRINGS.wallet_save_seed_and_connect}
            </Text>
          </Button>
          <Button
            onPress={() => navigation.navigate(STRINGS.navigation_wallet_create_seed)}
            negative>
            <Text style={[Typography.base, Typography.centered, Typography.negative]}>
              {STRINGS.wallet_no_seed}
            </Text>
          </Button>
        </View>
      </ScreenWrapper>
    </View>
  );
};

export default WalletSetSeed;

export const WalletSetSeedScreen: AppScreen = {
  id: STRINGS.navigation_wallet_insert_seed,
  title: STRINGS.navigation_wallet_insert_seed,
  component: WalletSetSeed,
};
