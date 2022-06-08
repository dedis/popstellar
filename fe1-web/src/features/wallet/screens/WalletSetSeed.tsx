import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import { TextInputLine, PoPTextButton } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppScreen } from 'core/navigation/AppNavigation';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { Color, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import * as Wallet from '../objects';

const styles = StyleSheet.create({
  welcomeView: {
    flex: 1,
    backgroundColor: Color.accent,
  } as ViewStyle,
});

type NavigationProps = StackScreenProps<
  AppParamList,
  typeof STRINGS.navigation_app_wallet_insert_seed
>;

/**
 * Wallet screen to set an already existing mnemonic
 */
const WalletSetSeed = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();

  const toast = useToast();

  /* used to set the mnemonic seed inserted by the user */
  const [seed, setSeed] = useState('');

  const initWallet = async () => {
    try {
      await Wallet.importMnemonic(seed);
      navigation.navigate(STRINGS.navigation_app_home, {
        screen: STRINGS.navigation_home_wallet,
        params: {
          screen: STRINGS.navigation_wallet_home,
        },
      });
    } catch (e) {
      console.error(e);
      toast.show(STRINGS.wallet_set_seed_error, {
        type: 'danger',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    }
  };

  return (
    <View style={styles.welcomeView}>
      <ScreenWrapper>
        <View>
          <Text style={[Typography.heading, Typography.negative]}>
            {STRINGS.wallet_restore_heading}
          </Text>
          <TextInputLine
            placeholder={STRINGS.wallet_restore_seed_example}
            onChangeText={(input: string) => setSeed(input)}
            negative
          />
          <PoPTextButton onPress={() => initWallet()} negative>
            {STRINGS.wallet_restore_using_known_seed}
          </PoPTextButton>
          <PoPTextButton
            onPress={() => navigation.navigate(STRINGS.navigation_app_wallet_create_seed)}
            negative>
            {STRINGS.wallet_previous_seed_not_known}
          </PoPTextButton>
        </View>
      </ScreenWrapper>
    </View>
  );
};

export default WalletSetSeed;

export const WalletSetSeedScreen: AppScreen = {
  id: STRINGS.navigation_app_wallet_insert_seed,
  title: STRINGS.navigation_app_wallet_insert_seed,
  Component: WalletSetSeed,
};
