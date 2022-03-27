import PropTypes from 'prop-types';
import React, { useEffect, useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { useSelector } from 'react-redux';

import { QRCode, TextBlock, WideButtonView } from 'core/components';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import { LaoEventType } from 'features/events/objects';
import { makeEventByTypeSelector } from 'features/events/reducer';
import { RollCall } from 'features/rollCall/objects';
import PROPS_TYPE from 'resources/Props';
import STRINGS from 'resources/strings';

import { RollCallTokensDropDown } from '../components';
import * as Wallet from '../objects';
import { createMockWalletState, clearMockWalletState } from '../objects/__mocks__/mockWallet';
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
  const [selectedToken, setSelectedToken] = useState<RollCallToken>();
  const [isDebug, setIsDebug] = useState(false);
  const rollCalls = useSelector(rollCallSelector);

  useEffect(() => {
    Wallet.recoverWalletRollCallTokens()
      .then((rct) => {
        setTokens(rct);
        setSelectedToken(rct[0]);
      })
      .catch((e) => {
        console.debug(e);
      });
  }, [rollCalls, isDebug]);

  const toggleDebugMode = () => {
    if (isDebug) {
      clearMockWalletState();
      setIsDebug(false);
    } else {
      createMockWalletState().then(() => setIsDebug(true));
    }
  };
  const tokenInfos = () => {
    if (selectedToken) {
      const rollCallName = `Roll Call name: ${selectedToken.rollCallId.valueOf()}`;
      return (
        <View style={containerStyles.centeredXY}>
          <TextBlock size={18} text={rollCallName} />
          <QRCode value={selectedToken.token.publicKey.valueOf()} visibility />
        </View>
      );
    }
    return <TextBlock text={STRINGS.no_tokens_in_wallet} />;
  };

  return (
    <View style={styles.homeContainer}>
      <TextBlock bold text={STRINGS.wallet_welcome} />
      <View style={styles.tokenSelectContainer}>
        {tokens && tokens.length > 0 && (
          <RollCallTokensDropDown rollCallTokens={tokens} onTokenChange={setSelectedToken} />
        )}
      </View>
      {selectedToken && tokenInfos()}
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
