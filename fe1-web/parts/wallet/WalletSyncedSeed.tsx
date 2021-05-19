import React, { useState } from 'react';
import {
  ScrollView,
  StyleSheet, View, ViewStyle,
} from 'react-native';
import styleContainer from 'styles/stylesheets/container';
import STRINGS from 'res/strings';
import TextBlock from 'components/TextBlock';
import WideButtonView from 'components/WideButtonView';
import { HDWallet } from 'model/objects/HDWallet';
import { WalletStore } from 'store/stores/WalletStore';
import { Hash } from 'model/objects';
import CopiableTextBlock from 'components/CopiableTextBlock';
import QRCode from 'components/QRCode';
import PROPS_TYPE from 'res/Props';
import PropTypes from 'prop-types';

const styles = StyleSheet.create({
  smallPadding: {
    padding: '1rem',
  } as ViewStyle,

  largePadding: {
    padding: '2rem',
  } as ViewStyle,
});

let cachedKeyPairs: Map<[Hash, Hash], string>;

/**
 * wallet UI once the wallet is synced
 * @constructor
 */
const WalletSyncedSeed = ({ navigation }: IPropTypes) => {
  /* boolean set to true if the token recover process is finished */
  const [tokensRecovered, setTokensRecovered] = useState(false);
  const [showPublicKey, setShowPublicKey] = useState(false);
  const [showQRPublicKey, setShowQRPublicKey] = useState(false);

  function hideStringButton() {
    return (
      <WideButtonView
        title={STRINGS.hide_public_keys}
        onPress={() => {
          setShowPublicKey(false);
        }}
      />
    );
  }

  function showStringButton() {
    return (
      <WideButtonView
        title={STRINGS.show_public_keys}
        onPress={() => {
          setShowPublicKey(true);
        }}
      />
    );
  }

  function hideQRButton() {
    return (
      <WideButtonView
        title={STRINGS.hide_qr_public_keys}
        onPress={() => {
          setShowQRPublicKey(false);
        }}
      />
    );
  }

  function showQRButton() {
    return (
      <WideButtonView
        title={STRINGS.show_qr_public_keys}
        onPress={() => {
          setShowQRPublicKey(true);
        }}
      />
    );
  }

  WalletStore.get().then((encryptedSeed) => HDWallet
    .fromState(encryptedSeed)
    .then((wallet) => {
      wallet.recoverTokens().then((cachedTokens) => {
        cachedKeyPairs = cachedTokens;
      });
    }));

  function showTokens() {
    const tokens: string[] = [];
    const laoAndRollCallId: string[] = [];

    let i = 0;
    cachedKeyPairs.forEach((value, key) => {
      const ids: string[] = key.toString().split(',');
      laoAndRollCallId[i] = `LAO ID           :  ${ids[0]} \n Roll Call ID      :  ${ids[1]}`;
      tokens[i] = value;
      i += 1;
    });

    return (
      <ScrollView>
        <View style={styles.largePadding} />
        <TextBlock bold text={STRINGS.your_tokens_title} />
        <View style={styles.smallPadding} />
        { laoAndRollCallId.map((value, key) => (
          <View>
            <View style={styles.smallPadding} />
            <TextBlock text={value} />
            <CopiableTextBlock id={key} text={tokens[key]} visibility={showPublicKey} />
            <View style={styles.smallPadding} />
            <QRCode value={tokens[key]} visibility={showQRPublicKey} />
          </View>
        ))}
        {!showPublicKey && showStringButton()}
        {showPublicKey && hideStringButton()}
        {!showQRPublicKey && showQRButton()}
        {showQRPublicKey && hideQRButton()}
        <WideButtonView
          title={STRINGS.back_to_wallet_home}
          onPress={() => navigation.navigate(STRINGS.navigation_home_tab_wallet)}
        />
        <View style={styles.largePadding} />
      </ScrollView>
    );
  }

  function recoverTokens() {
    return (
      <View>
        <TextBlock bold text={STRINGS.wallet_synced_info} />
        <View style={styles.largePadding} />
        <WideButtonView
          title={STRINGS.show_tokens_title}
          onPress={() => {
            setTokensRecovered(true);
          }}
        />
      </View>
    );
  }

  function getWalletDisplay() {
    return (
      <View style={styleContainer.centered}>
        {!tokensRecovered && recoverTokens()}
        {tokensRecovered && showTokens()}
      </View>
    );
  }

  return getWalletDisplay();
};

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};
WalletSyncedSeed.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default WalletSyncedSeed;
