import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import containerStyles from 'core/styles/stylesheets/containerStyles';
import { TextBlock, WideButtonView } from 'core/components';
import PROPS_TYPE from 'resources/Props';
import STRINGS from 'resources/strings';

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
      title={STRINGS.back_to_wallet_home}
      onPress={() => navigation.navigate(STRINGS.navigation_home_tab_wallet)}
    />
  </View>
);

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};
WalletError.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default WalletError;
