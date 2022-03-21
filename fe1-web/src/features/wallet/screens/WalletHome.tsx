import React, { useEffect, useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import PropTypes from 'prop-types';
import { useSelector } from 'react-redux';

import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';
import { QRCode, TextBlock, WideButtonView } from "core/components";
import PROPS_TYPE from 'resources/Props';
import { makeEventByTypeSelector } from 'features/events/reducer';
import { LaoEventType } from 'features/events/objects';
import { RollCall } from 'features/rollCall/objects';

import { useMockWalletState } from '../objects/__mocks__/mockWallet';
import * as Wallet from '../objects';
import { RollCallToken } from '../objects/RollCallToken';
import RollCallTokensDropDown from '../components/RollCallTokensDropDown';

const styles = StyleSheet.create({
  homeContainer: {
    ...containerStyles.centered,
    padding: '30px',
  } as ViewStyle,
  smallPadding: {
    padding: '1rem',
  } as ViewStyle,
  largePadding: {
    padding: '2rem',
  } as ViewStyle,
  rowContainer: {
    flexDirection: 'row',
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
  const mock = useMockWalletState();

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

  const setDebug = (isOn: boolean) => {
    if (isOn) {
      mock.clearMock();
      setIsDebug(false);
    } else {
      mock.useMock().then(() => setIsDebug(true));
    }
  };
  const tokenInfos = () => {
    if (selectedToken) {
      const laoId = 'Lao id: '.concat(selectedToken.laoId.valueOf());
      const rollCallId = 'Roll Call id: '.concat(selectedToken.rollCallId.valueOf());
      return (
        <View style={containerStyles.centered}>
          <TextBlock size={18} text={laoId} />
          <TextBlock size={18} text={rollCallId} />
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
        title={isDebug ? 'Set debug mode off' : 'Set debug mode on'}
        onPress={() => setDebug(isDebug)}
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
