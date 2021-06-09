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

  WalletStore.get().then((encryptedSeed) => HDWallet
    .fromState(encryptedSeed)
    .then((wallet) => {
      /*
       * TODO: recover the keys. For this the following map has to be passed to the
       *  recoverAllKeys function of the wallet (at the moment an EMPTY map is passed)
       *  [LAO ID 1, ROLL CALL ID 1] => [publicKey1, publicKey2, ... , publicKeyN]
       *  [LAO ID 1, ROLL CALL ID 2] => [publicKey1, publicKey2, ... , publicKeyN]
       *  .....
       *  [LAO ID N, ROLL CALL ID N] => [publicKey1, publicKey2, ... , publicKeyN]
       */

      /* =================================== REMOVE =================================== */
      // garbage effort river orphan negative kind outside quit hat camera approve first
      const laoId1: Hash = new Hash('T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=');
      const laoId2: Hash = new Hash('SyJ3d9TdH8Ycb4hPSGQdArTRIdP9Moywi1Ux/Kzav4o=');
      const rollCallId1: Hash = new Hash('T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=');
      const rollCallId2: Hash = new Hash('SyJ3d9TdH8Ycb4hPSGQdArTRIdP9Moywi1Ux/Kzav4o=');
      const testMap: Map<[Hash, Hash], string[]> = new Map();
      testMap.set([laoId1, rollCallId1], ['7147759d146897111bcf74f60a1948b1d3a22c9199a6b88c236eb7326adc2efc', '']);
      testMap.set([laoId2, rollCallId2], ['fffffffffffffff', '', 'ffdddddddffffffff', '2c23cfe90936a65839fb64dfb961690c3d8a5a1262f0156cf059b0c45a2eabff']);
      /* =================================== REMOVE =================================== */

      wallet.recoverAllKeys(testMap).then((cachedTokens) => {
        cachedKeyPairs = cachedTokens;
      });
    }));

  function showTokens() {
    const tokens: string[] = [];
    const laoId: string[] = [];
    const rollCallId: string[] = [];

    if (cachedKeyPairs.size === 0) {
      return (
        <View>
          <TextBlock text={STRINGS.no_tokens_in_wallet} />
          <View style={styles.largePadding} />
          <WideButtonView
            title={STRINGS.back_to_wallet_home}
            onPress={() => navigation.navigate(STRINGS.navigation_home_tab_wallet)}
          />
        </View>
      );
    }

    let i = 0;

    cachedKeyPairs.forEach((value, key) => {
      const ids: string[] = key.toString().split(',');
      // eslint-disable-next-line prefer-destructuring
      laoId[i] = ids[0];
      // eslint-disable-next-line prefer-destructuring
      rollCallId[i] = ids[1];
      tokens[i] = value;
      i += 1;
    });

    /* the below 4 functions are to manage user interaction with buttons */
    function hidePublicKeyButton() {
      return (
        <WideButtonView
          title={STRINGS.hide_public_keys}
          onPress={() => {
            setShowPublicKey(false);
          }}
        />
      );
    }

    function showPublicKeyButton() {
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

    /* this functions displays the LAOId the RollCallId and the public key generated from the two */
    function displayTokens() {
      return (
        <View>
          { laoId.map((value, key) => (
            <View key={value + 1} style={styleContainer.centered}>
              <View style={styles.smallPadding} />
              <TextBlock key={value + 2} bold text={STRINGS.lao_id} />
              <CopiableTextBlock key={value + 3} id={key} text={value} visibility />
              <TextBlock key={value + 4} bold text={STRINGS.roll_call_id} />
              <CopiableTextBlock key={value + 5} id={key} text={rollCallId[key]} visibility />
              <View style={styles.smallPadding} />
              <CopiableTextBlock
                key={value + 6}
                id={key}
                text={tokens[key]}
                visibility={showPublicKey}
              />
              <View style={styles.smallPadding} />
              <QRCode key={value + 7} value={tokens[key]} visibility={showQRPublicKey} />
            </View>
          ))}
        </View>
      );
    }

    return (
      <ScrollView>
        <View style={styles.largePadding} />
        <TextBlock bold text={STRINGS.your_tokens_title} />
        <View style={styles.smallPadding} />
        { displayTokens() }
        {!showPublicKey && showPublicKeyButton()}
        {showPublicKey && hidePublicKeyButton()}
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
