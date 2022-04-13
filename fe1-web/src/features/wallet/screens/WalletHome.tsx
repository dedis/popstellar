import PropTypes from 'prop-types';
import React, { useEffect, useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { useSelector } from 'react-redux';

import { QRCode, TextBlock, WideButtonView } from 'core/components';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import { LaoEventType } from 'features/events/objects';
import { makeEventByTypeSelector } from 'features/events/reducer';
import { selectCurrentLao } from 'features/lao/reducer';
import { RollCall } from 'features/rollCall/objects';
import PROPS_TYPE from 'resources/Props';
import STRINGS from 'resources/strings';

import { RollCallTokensDropDown } from '../components';
import * as Wallet from '../objects';
import { createDummyWalletState, clearDummyWalletState } from '../objects/DummyWallet';
import { RollCallToken } from '../objects/RollCallToken';

const styles = StyleSheet.create({
  homeContainer: {
    ...containerStyles.centeredXY,
    padding: '30px',
  } as ViewStyle,
  smallPadding: {
    padding: '1rem',
  } as ViewStyle,
  tokenSelectContainer: {
    flexDirection: 'row',
    marginTop: 30,
    marginBottom: 20,
  } as ViewStyle,
});

const rollCallSelector = makeEventByTypeSelector<RollCall>(LaoEventType.ROLL_CALL);

/**
 * Wallet UI once the wallet is synced
 */
const WalletHome = ({ navigation }: IPropTypes) => {
  const [tokens, setTokens] = useState<RollCallToken[]>();
  const [selectedTokenIndex, setSelectedTokenIndex] = useState(-1);
  const [isDebug, setIsDebug] = useState(false);
  const rollCalls = useSelector(rollCallSelector);
  const lao = useSelector(selectCurrentLao);

  useEffect(() => {
    if (lao && rollCalls[lao.id.valueOf()]) {
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
    } else {
      // Clear tokens screen state
      setSelectedTokenIndex(-1);
      setTokens(undefined);
    }
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
          <TextBlock size={18} text={rollCallName} />
          <QRCode value={tokens[selectedTokenIndex].token.publicKey.valueOf()} visibility />
        </View>
      );
    }
    return <TextBlock text={STRINGS.no_tokens_in_wallet} />;
  };

  return (
    <View style={styles.homeContainer}>
      <TextBlock bold text={STRINGS.wallet_welcome} />
      <View style={styles.tokenSelectContainer}>
        {tokens && (
          <RollCallTokensDropDown
            rollCallTokens={tokens}
            onIndexChange={setSelectedTokenIndex}
            selectedTokenIndex={selectedTokenIndex}
          />
        )}
      </View>
      {tokens && tokenInfos()}
      <View style={styles.smallPadding} />
      <WideButtonView
        title={STRINGS.logout_from_wallet}
        onPress={() => {
          Wallet.forget();
          navigation.navigate(STRINGS.navigation_wallet_setup_tab);
        }}
      />
      <WideButtonView
        title={(isDebug ? 'Set debug mode off' : 'Set debug mode on').concat(' [TESTING]')}
        onPress={() => toggleDebugMode()}
      />
    </View>
  );
};

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};
WalletHome.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default WalletHome;
