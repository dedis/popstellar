import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import PropTypes from 'prop-types';

import containerStyles from 'core/styles/stylesheets/containerStyles';
import { TextBlock, WideButtonView } from 'core/components';
import STRINGS from 'resources/strings';
import PROPS_TYPE from 'resources/Props';

const styles = StyleSheet.create({
  largePadding: {
    padding: '2rem',
  } as ViewStyle,
});

/**
 * Wallet synchronization error screen
 */
const WalletError = ({ navigation }: IPropTypes) => (
  <View style={containerStyles.centered}>
    <TextBlock text={STRINGS.wallet_error} />
    <View style={styles.largePadding} />
    <WideButtonView
      title={STRINGS.back_to_wallet_setup}
      onPress={() => navigation.navigate(STRINGS.navigation_wallet_setup_tab)}
    />
  </View>
);

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};
WalletError.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default WalletError;
