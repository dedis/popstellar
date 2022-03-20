import React, { ReactNode, useEffect, useState } from 'react';
import { ScrollView, StyleSheet, View, ViewStyle } from 'react-native';
import PropTypes from 'prop-types';
import { useSelector } from 'react-redux';

import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';
import { CopiableTextInput, QRCode, TextBlock, WideButtonView } from 'core/components';
import { PopToken } from 'core/objects';
import PROPS_TYPE from 'resources/Props';
import { selectLaosMap } from 'features/lao/reducer';
import { makeEventByTypeSelector } from 'features/events/reducer';
import { LaoEventType } from 'features/events/objects';
import { RollCall } from 'features/rollCall/objects';

import * as Wallet from '../objects';

const styles = StyleSheet.create({
  smallPadding: {
    padding: '1rem',
  } as ViewStyle,

  largePadding: {
    padding: '2rem',
  } as ViewStyle,
});

const rollCallSelector = makeEventByTypeSelector<RollCall>(LaoEventType.ROLL_CALL);

const hasTokens = (tokensByLao: Record<string, Record<string, PopToken>>): boolean =>
  tokensByLao && Object.values(tokensByLao).some((tokens) => Object.entries(tokens).length);

/**
 * Wallet UI once the wallet is synced
 */
const WalletSyncedSeed = ({ navigation }: IPropTypes) => {
  /* boolean set to true if the token recover process is finished */
  const [showTokens, setShowTokens] = useState(false);
  const [showQRPublicKey, setShowQRPublicKey] = useState(false);
  const [tokensByLao, setTokensByLao] = useState<Record<string, Record<string, PopToken>>>();

  const rollCalls = useSelector(rollCallSelector);
  const laos = useSelector(selectLaosMap);

  useEffect(() => {
    Wallet.recoverWalletPoPTokens()
      .then((kp) => {
        setTokensByLao(kp);
      })
      .catch((err) => console.debug(err));
  }, [rollCalls]);

  function displayOneToken(laoId: string, rollCallId: string): ReactNode {
    if (!tokensByLao) {
      console.warn('The tokensByLaoRollCall is undefined yet');
      return null;
    }
    const lao = laos[laoId];
    const rollCall = rollCalls[laoId][rollCallId];
    const tokenPk = tokensByLao[laoId][rollCallId].publicKey;

    return (
      <View style={containerStyles.centered} key={`token-${laoId}-${rollCallId}`}>
        <View style={styles.smallPadding} />
        <TextBlock bold text={`${STRINGS.lao_name}: ${lao.name}`} />
        <TextBlock bold text={`${STRINGS.roll_call_name}: ${rollCall.name}`} />
        <CopiableTextInput text={tokenPk.valueOf()} />
        <View style={styles.smallPadding} />
        <QRCode value={tokenPk.valueOf()} visibility={showQRPublicKey} />
      </View>
    );
  }

  const toggleQRButton = (
    <WideButtonView
      title={showQRPublicKey ? STRINGS.hide_qr_public_keys : STRINGS.show_qr_public_keys}
      onPress={() => {
        setShowQRPublicKey(!showQRPublicKey);
      }}
    />
  );

  const displayNoTokens = (
    <View>
      <TextBlock text={STRINGS.no_tokens_in_wallet} />
      <View style={styles.largePadding} />
      <WideButtonView title={STRINGS.back_to_wallet_home} onPress={() => setShowTokens(false)} />
      <WideButtonView
        title={STRINGS.logout_from_wallet}
        onPress={() => {
          Wallet.forget();
          navigation.navigate(STRINGS.navigation_home_tab_wallet);
        }}
      />
    </View>
  );

  function displayTokens() {
    if (!tokensByLao || !hasTokens(tokensByLao)) {
      return displayNoTokens;
    }

    return (
      <ScrollView>
        <View style={styles.largePadding} />
        <TextBlock bold text={STRINGS.your_tokens_title} />
        <View style={styles.smallPadding} />
        <View>
          {Object.keys(tokensByLao).map((laoId) =>
            Object.keys(tokensByLao[laoId]).map((rollCallId) => displayOneToken(laoId, rollCallId)),
          )}
        </View>
        {toggleQRButton}
        <WideButtonView title={STRINGS.back_to_wallet_home} onPress={() => setShowTokens(false)} />
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
    <View style={containerStyles.centered}>
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
