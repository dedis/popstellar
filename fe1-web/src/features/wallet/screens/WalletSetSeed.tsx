import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useState } from 'react';
import { Text, View } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import { TextInputLine } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { ToolbarItem } from 'core/components/Toolbar';
import { AppScreen } from 'core/navigation/AppNavigation';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import * as Wallet from '../objects';

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
        screen: STRINGS.navigation_home_home,
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

  const toolbarItems: ToolbarItem[] = [
    {
      title: STRINGS.wallet_previous_seed_not_known,
      onPress: () => navigation.navigate(STRINGS.navigation_app_wallet_create_seed),
      buttonStyle: 'secondary',
    },
    {
      title: STRINGS.wallet_restore_using_known_seed,
      onPress: initWallet,
    },
  ];

  return (
    <ScreenWrapper toolbarItems={toolbarItems}>
      <View>
        <Text style={Typography.heading}>{STRINGS.wallet_restore_heading}</Text>
        <Text style={Typography.paragraph}>{STRINGS.wallet_restore_instructions}</Text>
        <TextInputLine
          placeholder={STRINGS.wallet_restore_seed_example}
          onChangeText={(input: string) => setSeed(input)}
        />
      </View>
    </ScreenWrapper>
  );
};

export default WalletSetSeed;

export const WalletSetSeedScreen: AppScreen = {
  id: STRINGS.navigation_app_wallet_insert_seed,
  title: STRINGS.navigation_app_wallet_insert_seed,
  Component: WalletSetSeed,
};
