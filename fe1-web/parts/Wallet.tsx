import React, { useState } from 'react';
import {
  StyleSheet, TextInput, TextStyle, View, ViewStyle,
} from 'react-native';
import styleContainer from 'styles/stylesheets/container';
import STRINGS from 'res/strings';
import TextBlock from 'components/TextBlock';
import WideButtonView from 'components/WideButtonView';
import { Spacing, Typography } from 'styles';
import { HDWallet } from 'model/objects/HDWallet';

const styles = StyleSheet.create({
  textInput: {
    ...Typography.base,
    borderBottomWidth: 2,
    marginVertical: Spacing.s,
    marginHorizontal: Spacing.xl,
  } as TextStyle,
  viewTop: {
    justifyContent: 'flex-start',
  } as ViewStyle,
  viewBottom: {
    justifyContent: 'flex-end',
  } as ViewStyle,
});

const wallet: HDWallet = new HDWallet();

const Wallet = () => {
  /* this is the UI state of the wallet, starts null (namely homepage)
     the states can be: null | show_seed | set_seed | initialized */
  const [walletState, setWalletState] = useState(STRINGS.wallet_state_null);
  /* used to set the mnemonic seed inserted by the user */
  const [seed, setSeed] = useState('');
  /* boolean set to true if the token recover process is finished */
  const [tokensRecovered, setTokensRecovered] = useState(false);

  /* all the below are temporarily used for demonstration purposes (showing seed in UI) */
  const [showHideSeedTitle, setShowHideSeedTitle] = useState(STRINGS.show_seed_title);
  const [visibleSeed, setVisibleSeed] = useState(false);
  const [recoveredSeed, setRecoveredSeed] = useState('');
  // eslint-disable-next-line max-len
  const [showHideEncryptedSeedTitle, setShowHideEncryptedSeedTitle] = useState(STRINGS.show_encrypted_seed_title);
  const [visibleEncryptedSeed, setVisibleEncryptedSeed] = useState(false);
  const [recoveredEncryptedSeed, setRecoveredEncryptedSeed] = useState('');

  function getStartWalletDisplay() {
    return (
      <View style={styleContainer.centered}>
        <TextBlock bold text={STRINGS.welcome_to_wallet_display} />
        <TextBlock text={' '} />
        <TextBlock text={STRINGS.info_to_set_wallet} />
        <TextBlock text={' '} />
        <TextBlock text={STRINGS.caution_information_on_seed} />
        <TextBlock text={' '} />
        <TextBlock text={' '} />
        <WideButtonView
          title={STRINGS.create_new_wallet_button}
          onPress={() => {
            const generatedSeed = HDWallet.getNewGeneratedMnemonicSeed();
            console.log(generatedSeed);
            setSeed(generatedSeed);
            setWalletState(STRINGS.wallet_state_show_seed);
          }}
        />
        <WideButtonView
          title={STRINGS.import_seed_button}
          onPress={() => {
            setWalletState(STRINGS.wallet_state_set_seed);
          }}
        />
      </View>
    );
  }

  function getInsertSeedWalletDisplay() {
    return (
      <View style={styleContainer.centered}>
        <TextBlock text={STRINGS.type_seed_info} />
        <TextBlock text={' '} />
        <TextInput
          style={styles.textInput}
          placeholder={STRINGS.type_seed_example}
          onChangeText={(input: string) => setSeed(input)}
        />
        <TextBlock text={' '} />
        <WideButtonView
          title={STRINGS.setup_wallet}
          onPress={() => wallet.initialize(seed)
            .then((seedIsValid) => ((seedIsValid)
              ? setWalletState(STRINGS.wallet_state_initialized)
              : setWalletState(STRINGS.wallet_state_null)))}
        />
        <WideButtonView
          title={STRINGS.back_to_wallet_home}
          onPress={() => setWalletState(STRINGS.wallet_state_null)}
        />
      </View>
    );
  }

  function getShowSeedWalletDisplay() {
    return (
      <View style={styleContainer.centered}>
        <TextBlock bold text={STRINGS.show_seed_info} />
        <TextBlock text={' '} />
        <TextBlock text={seed} />
        <TextBlock text={' '} />
        <WideButtonView
          title={STRINGS.back_to_wallet_home}
          onPress={() => setWalletState(STRINGS.wallet_state_null)}
        />
      </View>
    );
  }

  function showHideSeed() {
    return (
      <View>
        <TextBlock visibility={visibleSeed} text={recoveredSeed} />
        <TextBlock text={' '} />
        <WideButtonView
          title={showHideSeedTitle}
          onPress={() => {
            setShowHideSeedTitle(visibleSeed ? STRINGS.show_seed_title : STRINGS.hide_seed_title);
            setVisibleSeed(!visibleSeed);
          }}
        />
        <TextBlock text={' '} />
        <TextBlock visibility={visibleEncryptedSeed} text={recoveredEncryptedSeed} />
        <TextBlock text={' '} />
        <WideButtonView
          title={showHideEncryptedSeedTitle}
          onPress={() => {
            setShowHideEncryptedSeedTitle(visibleEncryptedSeed
              ? STRINGS.show_encrypted_seed_title
              : STRINGS.hide_encrypted_seed_title);
            setVisibleEncryptedSeed(!visibleEncryptedSeed);
          }}
        />
      </View>
    );
  }

  function recoverTokens() {
    return (
      <View>
        <WideButtonView
          title={STRINGS.test_recover_seed_from_state}
          onPress={() => {
            setRecoveredEncryptedSeed(wallet.getEncryptedSeedToUint8Array().toString());
            wallet.getDecryptedSeed().then((s) => setRecoveredSeed(s.toString()))
              .catch((e) => console.log(e.toString()));
            setTokensRecovered(true);
          }}
        />
      </View>
    );
  }

  function getWalletDisplay() {
    return (
      <View style={styleContainer.centered}>
        <TextBlock bold text={STRINGS.wallet_synced_info} />
        <TextBlock text={' '} />
        { !tokensRecovered && recoverTokens() }
        { tokensRecovered && showHideSeed() }
      </View>
    );
  }

  function getDisplay() {
    return (
      <View style={styleContainer.centered}>
        { walletState === STRINGS.wallet_state_null && getStartWalletDisplay() }
        { walletState === STRINGS.wallet_state_show_seed && getShowSeedWalletDisplay() }
        { walletState === STRINGS.wallet_state_set_seed && getInsertSeedWalletDisplay() }
        { walletState === STRINGS.wallet_state_initialized && getWalletDisplay() }
      </View>
    );
  }

  return getDisplay();
};
export default Wallet;
