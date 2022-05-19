import { useNavigation } from '@react-navigation/native';
import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import { CopiableTextInput, Button } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppScreen } from 'core/navigation/AppNavigation';
import { Colors, Typography } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import * as Wallet from '../objects';
import { WalletStore } from '../store';

const styles = StyleSheet.create({
  welcomeView: {
    flex: 1,
    backgroundColor: Colors.accent,
  } as ViewStyle,
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
const WalletCreateSeed = () => {
  // FIXME: Navigation should use a defined type here (instead of any)
  const navigation = useNavigation<any>();

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
        navigation.navigate(STRINGS.app_navigation_tab_home, {
          screen: STRINGS.navigation_tab_home,
        });
      }
    });
  }, [navigation]);

  const connectWithSeed = async () => {
    try {
      await Wallet.importMnemonic(seed);
      navigation.navigate(STRINGS.app_navigation_tab_home, {
        screen: STRINGS.navigation_tab_home,
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
        <View style={containerStyles.centeredY}>
          <Text style={[Typography.heading, Typography.negative]}>
            {STRINGS.wallet_welcome_heading}
          </Text>
          <Text style={[Typography.paragraph, Typography.negative]}>
            {STRINGS.wallet_welcome_text_first_time}
          </Text>
          <Text style={Typography.paragraph}>
            <Text style={Typography.negative}>
              {STRINGS.wallet_welcome_text_wallet_explanation_1}
            </Text>
            <Text> </Text>
            <Text style={Typography.secondary}>
              {STRINGS.wallet_welcome_text_wallet_explanation_wallet}
            </Text>
            <Text> </Text>
            <Text style={Typography.negative}>
              {STRINGS.wallet_welcome_text_wallet_explanation_2}
            </Text>
            <Text> </Text>
            <Text style={Typography.secondary}>
              {STRINGS.wallet_welcome_text_wallet_explanation_seed}
            </Text>
            <Text style={Typography.negative}>. </Text>
            <Text style={Typography.negative}>
              {STRINGS.wallet_welcome_text_wallet_explanation_3}
            </Text>
            <Text> </Text>
            <Text style={Typography.secondary}>
              {STRINGS.wallet_welcome_text_wallet_explanation_seed}
            </Text>
            <Text style={Typography.negative}>
              {STRINGS.wallet_welcome_text_wallet_explanation_4}
            </Text>
          </Text>
          <CopiableTextInput text={seed} negative />
          <Text style={Typography.paragraph}>
            <Text style={Typography.negative}>
              {STRINGS.wallet_welcome_text_wallet_explanation_5}
            </Text>
            <Text> </Text>
            <Text style={Typography.secondary}>
              {STRINGS.wallet_welcome_text_wallet_explanation_seed}
            </Text>
            <Text style={Typography.negative}>
              {STRINGS.wallet_welcome_text_wallet_explanation_6}
            </Text>
          </Text>
          <Button onPress={() => connectWithSeed()} negative>
            <Text style={[Typography.base, Typography.centered, Typography.negative]}>
              {STRINGS.wallet_welcome_start_exploring}
            </Text>
          </Button>
          <Button
            onPress={() => navigation.navigate(STRINGS.navigation_wallet_insert_seed)}
            negative>
            <Text style={[Typography.base, Typography.centered, Typography.negative]}>
              {STRINGS.wallet_welcome_already_know_seed}
            </Text>
          </Button>
        </View>
      </ScreenWrapper>
    </View>
  );
};
export default WalletCreateSeed;

export const WalletCreateSeedScreen: AppScreen = {
  id: STRINGS.navigation_wallet_create_seed,
  title: STRINGS.navigation_wallet_create_seed,
  component: WalletCreateSeed,
};
