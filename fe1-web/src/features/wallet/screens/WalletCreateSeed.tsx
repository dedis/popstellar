import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, TextStyle, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import { PoPTextButton } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppScreen } from 'core/navigation/AppNavigation';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { Border, Color, Spacing, Typography } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import * as Wallet from '../objects';
import { WalletStore } from '../store';

const styles = StyleSheet.create({
  welcomeView: {
    flex: 1,
    backgroundColor: Color.accent,
  } as ViewStyle,
  words: {
    flex: 1,
    flexWrap: 'wrap',
    flexDirection: 'row',
    borderColor: Color.contrast,
    borderWidth: Border.width,
    borderRadius: Border.radius,
    backgroundColor: Color.secondaryAccent,
    marginBottom: Spacing.x1,
    paddingVertical: Spacing.x05,
  } as ViewStyle,
  word: {
    ...Typography.base,
    ...Typography.code,
    color: Color.contrast,
    paddingHorizontal: Spacing.x05,
    paddingVertical: Spacing.x025,
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
          <View style={styles.words}>
            {seed.split(' ').map((word, idx) => (
              <Text key={idx.toString()} style={styles.word}>
                {word}
              </Text>
            ))}
          </View>
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
          <PoPTextButton onPress={() => connectWithSeed()} negative>
            {STRINGS.wallet_welcome_start_exploring}
          </PoPTextButton>
          <PoPTextButton
            onPress={() => navigation.navigate(STRINGS.navigation_app_wallet_insert_seed)}
            negative>
            {STRINGS.wallet_welcome_already_know_seed}
          </PoPTextButton>
        </View>
      </ScreenWrapper>
    </View>
  );
};
export default WalletCreateSeed;

export const WalletCreateSeedScreen: AppScreen = {
  id: STRINGS.navigation_app_wallet_create_seed,
  title: STRINGS.navigation_app_wallet_create_seed,
  Component: WalletCreateSeed,
};
