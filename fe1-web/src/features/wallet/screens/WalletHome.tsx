import { useNavigation } from '@react-navigation/native';
import React, { useEffect, useState } from 'react';
import {
  StyleSheet,
  View,
  ViewStyle,
  Text,
  TextStyle,
  TouchableOpacity,
  Modal,
} from 'react-native';
import { Icon } from 'react-native-elements';
import { useSelector } from 'react-redux';

import { QRCode, WideButtonView } from 'core/components';
import { Typography } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import { LaoEventType } from 'features/events/objects';
import { makeEventByTypeSelector } from 'features/events/reducer';
import { selectCurrentLao } from 'features/lao/reducer';
import { RollCall } from 'features/rollCall/objects';
import STRINGS from 'resources/strings';

import { RollCallTokensDropDown } from '../components';
import RoundIconButton from '../components/RoundIconButton';
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

const rollCallSelector = makeEventByTypeSelector<RollCall>(LaoEventType.ROLL_CALL);

/**
 * Wallet UI once the wallet is synced
 */
const WalletHome = () => {
  const [modalVisible, setModalVisible] = useState(false);
  const [tokens, setTokens] = useState<RollCallToken[]>();
  const [selectedTokenIndex, setSelectedTokenIndex] = useState(-1);
  const [isDebug, setIsDebug] = useState(false);
  const rollCalls = useSelector(rollCallSelector);
  const lao = useSelector(selectCurrentLao);

  // FIXME: Navigation should use a defined type here (instead of any)
  const navigation = useNavigation<any>();

  useEffect(() => {
    if (!lao || !rollCalls[lao.id.valueOf()]) {
      // Clear tokens screen state
      setSelectedTokenIndex(-1);
      setTokens(undefined);
      return;
    }

    Wallet.recoverWalletRollCallTokens(rollCalls, lao)
      .then((rct) => {
        if (rct.length > 0) {
          setTokens(rct);
          setSelectedTokenIndex(0);
        }
      })
      .catch((e) => {
        console.debug(e);
      });
  }, [rollCalls, isDebug, lao]);

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
      <View style={styles.mainButtonsContainer}>
        <RoundIconButton name="send" onClick={() => {setModalVisible(true)}} />
        <RoundIconButton name="search" onClick={() => {}} />
        <RoundIconButton name="history" onClick={() => {}} />
      </View>
      <Modal
        animationType="slide"
        visible={modalVisible}
        onRequestClose={() => {
          alert('Modal has been closed.');
          setModalVisible(!modalVisible);
        }}>
        <Text>Hello World!</Text>
      </Modal>
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
    </View>
  );
};

export default WalletHome;
