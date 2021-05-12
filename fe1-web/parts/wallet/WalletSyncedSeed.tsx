import React, { useState } from 'react';
import {
  StyleSheet, View, ViewStyle,
} from 'react-native';
import styleContainer from 'styles/stylesheets/container';
import STRINGS from 'res/strings';
import TextBlock from 'components/TextBlock';
import WideButtonView from 'components/WideButtonView';
import { HDWallet } from 'model/objects/HDWallet';
import { WalletStore } from 'store/stores/WalletStore';
import { Hash } from 'model/objects';

const styles = StyleSheet.create({
  largePadding: {
    padding: '2rem',
  } as ViewStyle,
});

let cachedKeyPairs: Map<[Hash, Hash], string>;

/**
 * wallet UI once the wallet is synced
 * @constructor
 */
const WalletSyncedSeed = () => {
  /* boolean set to true if the token recover process is finished */
  const [tokensRecovered, setTokensRecovered] = useState(false);

  WalletStore.get().then((encryptedSeed) => HDWallet
    .fromState(encryptedSeed)
    .then((wallet) => {
      wallet.recoverTokens().then((cachedTokens) => {
        cachedKeyPairs = cachedTokens;
      });
    }));

  function showTokens() {
    const tokens: string[] = [];

    cachedKeyPairs.forEach((value, key) => {
      tokens[0] = value;
      tokens[1] = value;
      tokens[2] = value;
    });

    return (
      <View>
        <TextBlock text={tokens[0]} />
        <TextBlock text={tokens[1]} />
        <TextBlock text={tokens[2]} />
      </View>
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

export default WalletSyncedSeed;
