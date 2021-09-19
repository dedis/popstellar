import React, { useState } from 'react';
import {
  ScrollView,
  StyleSheet, View, ViewStyle,
} from 'react-native';
import styleContainer from 'styles/stylesheets/container';
import STRINGS from 'res/strings';
import TextBlock from 'components/TextBlock';
import WideButtonView from 'components/WideButtonView';
import {
  LaoEventType, RollCall, PopToken, Wallet,
} from 'model/objects';
import CopiableTextBlock from 'components/CopiableTextBlock';
import QRCode from 'components/QRCode';
import PROPS_TYPE from 'res/Props';
import PropTypes from 'prop-types';
import { makeEventByTypeSelector, makeLaosMap } from 'store';
import { useSelector } from 'react-redux';

const styles = StyleSheet.create({
  smallPadding: {
    padding: '1rem',
  } as ViewStyle,

  largePadding: {
    padding: '2rem',
  } as ViewStyle,
});

/**
 * wallet UI once the wallet is synced
 * @constructor
 */
const WalletSyncedSeed = ({ navigation }: IPropTypes) => {
  /* boolean set to true if the token recover process is finished */
  const [showTokens, setShowTokens] = useState(false);
  const [showPublicKey, setShowPublicKey] = useState(false);
  const [showQRPublicKey, setShowQRPublicKey] = useState(false);

  const rollCallSelector = makeEventByTypeSelector<RollCall>(LaoEventType.ROLL_CALL);
  const rollCalls = useSelector(rollCallSelector);

  const laoSelector = makeLaosMap();
  const laos = useSelector(laoSelector);

  let tokensByLaoRollCall: Record<string, Record<string, PopToken>> = {};

  Wallet.recoverWalletPoPTokens()
    .then((kp) => {
      tokensByLaoRollCall = kp;
    })
    .catch((err) => console.debug(err));

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

  function displayNoTokens() {
    return (
      <View>
        <TextBlock text={STRINGS.no_tokens_in_wallet} />
        <View style={styles.largePadding} />
        <WideButtonView
          title={STRINGS.back_to_wallet_home}
          onPress={() => setShowTokens(false)}
        />
        <WideButtonView
          title={STRINGS.logout_from_wallet}
          onPress={() => {
            Wallet.forget();
            navigation.navigate(STRINGS.navigation_home_tab_wallet);
          }}
        />
      </View>
    );
  }

  function displayOneToken(laoId: string, rollCallId: string) {
    const lao = laos[laoId];
    const rollCall = rollCalls[laoId][rollCallId];
    const tokenPk = tokensByLaoRollCall[laoId][rollCallId].publicKey;

    return (
      <View style={styleContainer.centered}>
        <View style={styles.smallPadding} />
        <TextBlock bold text={STRINGS.lao_id} />
        <CopiableTextBlock text={lao.name} visibility />
        <TextBlock bold text={STRINGS.roll_call_name} />
        <TextBlock text={rollCall.name} visibility />
        <View style={styles.smallPadding} />
        <CopiableTextBlock text={tokenPk.valueOf()} visibility={showPublicKey} />
        <View style={styles.smallPadding} />
        <QRCode value={tokenPk.valueOf()} visibility={showQRPublicKey} />
      </View>
    );
  }

  function displayTokens() {
    if (Object.keys(tokensByLaoRollCall).length === 0) {
      return displayNoTokens();
    }

    return (
      <ScrollView>
        <View style={styles.largePadding} />
        <TextBlock bold text={STRINGS.your_tokens_title} />
        <View style={styles.smallPadding} />
        <View>
          {
            Object.keys(tokensByLaoRollCall).map(
              (laoId) => Object.keys(tokensByLaoRollCall[laoId]).map(
                (rollCallId) => displayOneToken(laoId, rollCallId),
              ),
            )
          }
        </View>
        {!showPublicKey && showPublicKeyButton()}
        {showPublicKey && hidePublicKeyButton()}
        {!showQRPublicKey && showQRButton()}
        {showQRPublicKey && hideQRButton()}
        <WideButtonView
          title={STRINGS.back_to_wallet_home}
          onPress={() => setShowTokens(false)}
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
            setShowTokens(true);
          }}
        />
        <WideButtonView
          title={STRINGS.logout_from_wallet}
          onPress={() => {
            Wallet.forget();
            navigation.navigate(STRINGS.navigation_home_tab_wallet);
          }}
        />
      </View>
    );
  }

  return (
    <View style={styleContainer.centered}>
      {!showTokens && recoverTokens()}
      {showTokens && displayTokens()}
    </View>
  );
};

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};
WalletSyncedSeed.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default WalletSyncedSeed;
