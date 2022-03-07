import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { useNavigation } from '@react-navigation/native';

import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';
import { TextBlock, WideButtonView } from 'core/components';

import { WalletStore } from '../store';

const styles = StyleSheet.create({
  smallPadding: {
    padding: '1rem',
  } as ViewStyle,
  largePadding: {
    padding: '2rem',
  } as ViewStyle,
});

/**
 * Wallet home screen
 */
const WalletSetup = () => {
  // FIXME: Navigation should use a defined type here (instead of any)
  const navigation = useNavigation<any>();

  function importSeed() {
    if (WalletStore.hasSeed()) {
      navigation.navigate(STRINGS.navigation_wallet_synced);
    } else {
      navigation.navigate(STRINGS.navigation_wallet_insert_seed);
    }
  }

  return (
    <View style={containerStyles.centered}>
      <TextBlock bold text={STRINGS.welcome_to_wallet_display} />
      <View style={styles.smallPadding} />
      <TextBlock text={STRINGS.info_to_set_wallet} />
      <TextBlock text={STRINGS.caution_information_on_seed} />
      <View style={styles.largePadding} />
      <WideButtonView
        title={STRINGS.create_new_wallet_button}
        onPress={() => navigation.navigate(STRINGS.navigation_wallet_create_seed)}
      />
      <WideButtonView title={STRINGS.import_seed_button} onPress={() => importSeed()} />
    </View>
  );
};

export default WalletSetup;
