import { useNavigation } from '@react-navigation/native';
import React, { useEffect, useState } from 'react';
import { StyleSheet, View, ViewStyle, Text, TextStyle, TextInput } from 'react-native';
import { useSelector } from 'react-redux';

import { QRCode, WideButtonView } from 'core/components';
import { KeyPairStore } from 'core/keypair';
import { Typography } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';

import { RollCallTokensDropDown, SendModal, RoundIconButton } from '../components';
import { WalletHooks } from '../hooks';
import { WalletFeature } from '../interface';
import { requestCoinbaseTransaction } from '../network';
import * as Wallet from '../objects';
import { createDummyWalletState, clearDummyWalletState } from '../objects/DummyWallet';
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
  mainButtonsContainer: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-around',
    minWidth: '50%',
    margin: 'auto',
    marginTop: 20,
  } as ViewStyle,
  textBase: Typography.base as TextStyle,
  textImportant: Typography.important as TextStyle,
});

/**
 * Wallet UI once the wallet is synced
 */
const WalletHome = () => {
  const [sendValue, setSendValue] = useState(0);
  const [sendModalVisible, setSendModalVisible] = useState(false);
  const [tokens, setTokens] = useState<RollCallToken[]>();
  const [selectedTokenIndex, setSelectedTokenIndex] = useState(-1);
  const [isDebug, setIsDebug] = useState(false);

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
  }, [rollCalls, isDebug, laoId]);

  const toggleDebugMode = () => {
    if (isDebug) {
      clearDummyWalletState();
      setIsDebug(false);
    } else {
      createDummyWalletState().then(() => setIsDebug(true));
    }
  };

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

  const sendField = () => {
    return (
      <>
        <WideButtonView
          title={STRINGS.cash_send}
          onPress={() => {
            requestCoinbaseTransaction(
              KeyPairStore.get(),
              tokens![selectedTokenIndex].token.publicKey,
              sendValue,
            );
          }}
        />
        <TextInput
          onChangeText={(t) => {
            setSendValue(Number.parseInt(t, 10));
          }}
        />
      </>
    );
  };

  return (
    <View style={styles.homeContainer}>
      <Text style={styles.textImportant}>{STRINGS.wallet_welcome}</Text>
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
      {tokens && (
        <View style={styles.mainButtonsContainer}>
          <RoundIconButton
            name="send"
            onClick={() => {
              setSendModalVisible(true);
            }}
          />
        </View>
      )}
      <SendModal modalVisible={sendModalVisible} setModalVisible={setSendModalVisible} />
      <View style={styles.smallPadding} />
      <WideButtonView
        title={STRINGS.logout_from_wallet}
        onPress={() => {
          Wallet.forget();
          navigation.reset({
            index: 0,
            routes: [{ name: STRINGS.navigation_wallet_setup_tab }],
          });
        }}
      />
      <WideButtonView
        title={(isDebug ? 'Set debug mode off' : 'Set debug mode on').concat(' [TESTING]')}
        onPress={() => toggleDebugMode()}
      />
      {tokens && sendField()}
    </View>
  );
};

export default WalletHome;
