import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import PropTypes from 'prop-types';

import containerStyles from 'styles/stylesheets/containerStyles';
import TextBlock from 'core/components/TextBlock';
import STRINGS from 'res/strings';
import WideButtonView from 'core/components/WideButtonView';
import PROPS_TYPE from 'res/Props';

const styles = StyleSheet.create({
  largePadding: {
    padding: '2rem',
  } as ViewStyle,
});

/**
 * Wallet synchronization error screen
 */
const WalletError = ({ navigation }: IPropTypes) => {
  function getWalletErrorDisplay() {
    return (
      <View style={containerStyles.centered}>
        <TextBlock text={STRINGS.wallet_error} />
        <View style={styles.largePadding} />
        <WideButtonView
          title={STRINGS.back_to_wallet_home}
          onPress={() => navigation.navigate(STRINGS.navigation_home_tab_wallet)}
        />
      </View>
    );
  }

  return getWalletErrorDisplay();
};

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};
WalletError.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default WalletError;
