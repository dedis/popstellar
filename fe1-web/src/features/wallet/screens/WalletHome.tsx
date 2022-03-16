import { useNavigation } from '@react-navigation/native';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { TextBlock, WideButtonView } from 'core/components';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';

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
const WalletHome = () => {
  // FIXME: Navigation should use a defined type here (instead of any)
  const navigation = useNavigation<any>();

  function importSeed() {
    if (WalletStore.hasSeed()) {
      navigation.navigate(STRINGS.navigation_synced_wallet);
    } else {
      navigation.navigate(STRINGS.navigation_insert_seed_tab_wallet);
    }
  }

  return (
    <View style={containerStyles.centeredY}>
      <TextBlock bold text={STRINGS.welcome_to_wallet_display} />
      <View style={styles.smallPadding} />
      <TextBlock text={STRINGS.info_to_set_wallet} />
      <TextBlock text={STRINGS.caution_information_on_seed} />
      <View style={styles.largePadding} />
      <WideButtonView
        title={STRINGS.create_new_wallet_button}
        onPress={() => navigation.navigate(STRINGS.navigation_show_seed_wallet)}
      />
      <WideButtonView title={STRINGS.import_seed_button} onPress={() => importSeed()} />
    </View>
  );
};

export default WalletHome;
