import { useNavigation } from '@react-navigation/native';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { TextBlock, WideButtonView } from 'core/components';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';

const styles = StyleSheet.create({
  largePadding: {
    padding: '2rem',
  } as ViewStyle,
});

/**
 * Wallet synchronization error screen
 */
const WalletError = () => {
  const navigation = useNavigation<any>();
  return (
    <View style={containerStyles.centeredY}>
      <TextBlock text={STRINGS.wallet_error} />
      <View style={styles.largePadding} />
      <WideButtonView
        title={STRINGS.back_to_wallet_setup}
        onPress={() => navigation.navigate(STRINGS.navigation_wallet_setup_tab)}
      />
    </View>
  );
};

export default WalletError;
