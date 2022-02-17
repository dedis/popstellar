import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import containerStyles from 'styles/stylesheets/containerStyles';
import STRINGS from 'res/strings';
import TextBlock from 'components/TextBlock';
import WideButtonView from 'components/WideButtonView';
import PROPS_TYPE from 'res/Props';
import PropTypes from 'prop-types';
import { Wallet } from 'model/objects';
import CopiableTextInput from 'components/CopiableTextInput';

const styles = StyleSheet.create({
  smallPadding: {
    padding: '1rem',
  } as ViewStyle,
  largePadding: {
    padding: '2rem',
  } as ViewStyle,
});

/**
 * wallet screen to obtain a new mnemonic seed
 * @constructor
 */
const WalletShowSeed = ({ navigation }: IPropTypes) => {
  /* used to set the mnemonic seed inserted by the user */
  const seed: string = Wallet.generateMnemonicSeed();

  function getShowSeedWalletDisplay() {
    return (
      <View style={containerStyles.centered}>
        <TextBlock bold text={STRINGS.show_seed_info} />
        <View style={styles.smallPadding} />
        <CopiableTextInput text={seed} />
        <View style={styles.smallPadding} />
        <WideButtonView
          title={STRINGS.back_to_wallet_home}
          onPress={() => navigation.navigate(STRINGS.navigation_home_tab_wallet)}
        />
      </View>
    );
  }

  return getShowSeedWalletDisplay();
};

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};
WalletShowSeed.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default WalletShowSeed;
