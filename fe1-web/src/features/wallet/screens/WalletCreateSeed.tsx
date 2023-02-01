import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, TextStyle, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import { CopyButton } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { ToolbarItem } from 'core/components/Toolbar';
import { AppScreen } from 'core/navigation/AppNavigation';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { Border, Color, Spacing, Typography } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import * as Wallet from '../objects';
import { WalletStore } from '../store';

const styles = StyleSheet.create({
  words: {
    borderRadius: Border.radius,
    backgroundColor: Color.accent,
    marginBottom: Spacing.x1,
    padding: Spacing.x1,

    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  } as ViewStyle,
  seed: {
    ...Typography.base,
    ...Typography.code,
    color: Color.contrast,
  } as TextStyle,
});

type NavigationProps = StackScreenProps<
  AppParamList,
  typeof STRINGS.navigation_app_wallet_create_seed
>;

/**
 * Wallet screen to obtain a new mnemonic seed
 */
const WalletCreateSeed = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();

  const toast = useToast();

  /* used to set the mnemonic seed inserted by the user */
  const [seed, setSeed] = useState('');

  useEffect(() => {
    setSeed(Wallet.generateMnemonicSeed());
  }, []);

  useEffect(() => {
    // Return the function to unsubscribe from the event so it gets removed on unmount
    return navigation.addListener('focus', () => {
      if (WalletStore.hasSeed()) {
        navigation.navigate(STRINGS.navigation_app_home, {
          screen: STRINGS.navigation_home_home,
        });
      }
    });
  }, [navigation]);

  const connectWithSeed = async () => {
    try {
      await Wallet.importMnemonic(seed);
      navigation.navigate(STRINGS.navigation_app_home, {
        screen: STRINGS.navigation_home_home,
      });
    } catch (e) {
      console.error(e);
      toast.show(STRINGS.wallet_set_seed_error, {
        type: 'danger',
        placement: 'bottom',
        duration: FOUR_SECONDS,
      });
    }
  };

  const toolbarItems: ToolbarItem[] = [
    {
      title: STRINGS.wallet_welcome_button_restore_seed,
      onPress: () => navigation.navigate(STRINGS.navigation_app_wallet_insert_seed),
      buttonStyle: 'secondary',
    },
    {
      id: 'exploring_selector',
      title: STRINGS.wallet_welcome_button_start_exploring,
      onPress: connectWithSeed,
    },
  ];

  return (
    <ScreenWrapper toolbarItems={toolbarItems}>
      <View style={containerStyles.centeredY}>
        <Text style={Typography.heading}>{STRINGS.wallet_welcome_heading}</Text>
        <Text style={Typography.paragraph}>{STRINGS.wallet_welcome_text_first_time}</Text>
        <Text style={Typography.paragraph}>
          <Text>{STRINGS.wallet_welcome_text_wallet_explanation_1}</Text>
          <Text> </Text>
          <Text style={Typography.accent}>
            {STRINGS.wallet_welcome_text_wallet_explanation_wallet}
          </Text>
          <Text> </Text>
          <Text>{STRINGS.wallet_welcome_text_wallet_explanation_2}</Text>
          <Text> </Text>
          <Text style={Typography.accent}>
            {STRINGS.wallet_welcome_text_wallet_explanation_seed}
          </Text>
          <Text>. </Text>
          <Text>{STRINGS.wallet_welcome_text_wallet_explanation_3}</Text>
          <Text> </Text>
          <Text style={Typography.accent}>
            {STRINGS.wallet_welcome_text_wallet_explanation_seed}
          </Text>
          <Text>{STRINGS.wallet_welcome_text_wallet_explanation_4}</Text>
        </Text>
        <View style={styles.words}>
          <Text style={styles.seed} selectable>
            {seed}
          </Text>
          <CopyButton data={seed} negative />
        </View>
        <Text style={Typography.paragraph}>
          <Text>{STRINGS.wallet_welcome_text_wallet_explanation_5}</Text>
          <Text> </Text>
          <Text style={Typography.accent}>
            {STRINGS.wallet_welcome_text_wallet_explanation_seed}
          </Text>
          <Text>{STRINGS.wallet_welcome_text_wallet_explanation_6}</Text>
        </Text>
        <Text style={Typography.paragraph}>
          <Text>{STRINGS.wallet_welcome_already_know_seed_1}</Text>
          <Text> </Text>
          <Text style={Typography.accent}>
            {STRINGS.wallet_welcome_text_wallet_explanation_seed}
          </Text>
          <Text>{STRINGS.wallet_welcome_already_know_seed_2}</Text>
          <Text> </Text>
          <Text>{STRINGS.wallet_welcome_start_exploring}</Text>
          <Text> </Text>
          <Text style={Typography.accent}>
            {STRINGS.wallet_welcome_text_wallet_explanation_wallet}
          </Text>
          <Text>.</Text>
        </Text>
      </View>
    </ScreenWrapper>
  );
};
export default WalletCreateSeed;

export const WalletCreateSeedScreen: AppScreen = {
  id: STRINGS.navigation_app_wallet_create_seed,
  title: STRINGS.navigation_app_wallet_create_seed,
  Component: WalletCreateSeed,
};
