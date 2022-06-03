import { useNavigation } from '@react-navigation/native';
import React, { useEffect, useState } from 'react';
import { StyleSheet, View, ViewStyle, Text, TextStyle } from 'react-native';
import { useSelector } from 'react-redux';

import { LogoutRoundButton, QRCode } from 'core/components';
import { Typography } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';

import { RollCallTokensDropDown } from '../components';
import { WalletHooks } from '../hooks';
import { WalletFeature } from '../interface';
import * as Wallet from '../objects';
import { RollCallToken } from '../objects/RollCallToken';

const styles = StyleSheet.create({
  homeContainer: {
    ...containerStyles.centeredXY,
    padding: 30,
  } as ViewStyle,
  smallPadding: {
    padding: '1rem',
  } as ViewStyle,
  tokenSelectContainer: {
    flexDirection: 'row',
    marginTop: 30,
    marginBottom: 20,
  } as ViewStyle,
  textBase: Typography.baseCentered as TextStyle,
  textImportant: Typography.important as TextStyle,
  topBar: {
    display: 'flex',
    flexDirection: 'row',
    width: '100%',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingLeft: 20,
    paddingRight: 20,
  } as ViewStyle,
});

/**
 * Wallet UI once the wallet is synced
 */
const WalletHome = () => {
  const [tokens, setTokens] = useState<RollCallToken[]>();
  const [selectedTokenIndex, setSelectedTokenIndex] = useState(-1);

  const rollCallSelector = WalletHooks.useEventByTypeSelector<WalletFeature.RollCall>(
    WalletFeature.EventType.ROLL_CALL,
  );
  const rollCalls = useSelector(rollCallSelector);

  const laoId = WalletHooks.useCurrentLaoId();

  // FIXME: Navigation should use a defined type here (instead of any)
  const navigation = useNavigation<any>();

  useEffect(() => {
    if (!laoId || !rollCalls || !rollCalls[laoId.valueOf()]) {
      // Clear tokens screen state
      setSelectedTokenIndex(-1);
      setTokens(undefined);
      return;
    }

    Wallet.recoverWalletRollCallTokens(rollCalls, laoId)
      .then((rct) => {
        if (rct.length > 0) {
          setTokens(rct);
          setSelectedTokenIndex(0);
        }
      })
      .catch((e) => {
        console.debug(e);
      });
  }, [rollCalls, laoId]);

  const tokenInfos = () => {
    if (selectedTokenIndex !== -1 && tokens) {
      const rollCallName = `Roll Call name: ${tokens[selectedTokenIndex].rollCallName.valueOf()}`;
      return (
        <View style={containerStyles.centeredXY}>
          <Text style={styles.textBase}>{rollCallName}</Text>
          <QRCode value={tokens[selectedTokenIndex].token.publicKey.valueOf()} visibility />
        </View>
      );
    }
    return <Text style={styles.textBase}>{STRINGS.no_tokens_in_wallet}</Text>;
  };

  return (
    <View style={styles.homeContainer}>
      <View style={styles.topBar}>
        <Text style={styles.textImportant}>{STRINGS.wallet_welcome}</Text>
        <LogoutRoundButton
          onClick={() => {
            Wallet.forget();
            navigation.reset({
              index: 0,
              routes: [{ name: STRINGS.navigation_wallet_setup_tab }],
            });
          }}
        />
      </View>
      <View style={styles.tokenSelectContainer}>
        {tokens && (
          <RollCallTokensDropDown
            rollCallTokens={tokens}
            onIndexChange={setSelectedTokenIndex}
            selectedTokenIndex={selectedTokenIndex}
          />
        )}
      </View>
      {tokenInfos()}
      <View style={styles.smallPadding} />
    </View>
  );
};

export default WalletHome;
