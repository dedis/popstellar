import React, { useEffect, useState } from 'react';
import { StyleSheet, View, ViewStyle, Text } from 'react-native';

import { QRCode } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { RollCallTokensDropDown } from '../components';
import { WalletHooks } from '../hooks';
import * as Wallet from '../objects';
import { RollCallToken } from '../objects/RollCallToken';

const styles = StyleSheet.create({
  tokenSelectContainer: {
    flexDirection: 'row',
    marginTop: 30,
    marginBottom: 20,
  } as ViewStyle,
});

/**
 * Wallet UI once the wallet is synced
 */
const WalletHome = () => {
  const [tokens, setTokens] = useState<RollCallToken[]>([]);
  const [selectedTokenIndex, setSelectedTokenIndex] = useState(-1);

  const rollCalls = WalletHooks.useRollCallsByLaoId();

  const laoId = WalletHooks.useCurrentLaoId();

  useEffect(() => {
    let updateWasCanceled = false;

    if (!laoId || !rollCalls || !rollCalls[laoId.valueOf()]) {
      // Clear tokens screen state
      setTokens([]);
      setSelectedTokenIndex(-1);
    } else {
      // this can cause problems since it is async. some updates can be lost
      // depending on the interleaving
      Wallet.recoverWalletRollCallTokens(rollCalls, laoId)
        .then((rct) => {
          if (!updateWasCanceled) {
            setTokens(rct);
            if (rct.length > 0) {
              setSelectedTokenIndex(0);
            } else {
              setSelectedTokenIndex(-1);
            }
          }
        })
        .catch((e) => {
          console.debug(e);
        });
    }

    return () => {
      // cancel update if the hook is called again
      updateWasCanceled = true;
    };
  }, [rollCalls, laoId]);

  const tokenInfos = () => {
    if (selectedTokenIndex !== -1) {
      const rollCallName = `Roll Call name: ${tokens[selectedTokenIndex].rollCallName.valueOf()}`;
      return (
        <>
          <Text style={Typography.base}>{rollCallName}</Text>
          <QRCode value={tokens[selectedTokenIndex].token.publicKey.valueOf()} visibility />
        </>
      );
    }
    return <Text style={Typography.paragraph}>{STRINGS.no_tokens_in_wallet}</Text>;
  };

  return (
    <ScreenWrapper>
      <Text style={Typography.heading}>{STRINGS.wallet_home_header}</Text>
      {selectedTokenIndex !== -1 && (
        <View style={styles.tokenSelectContainer}>
          <RollCallTokensDropDown
            rollCallTokens={tokens}
            onIndexChange={setSelectedTokenIndex}
            selectedTokenIndex={selectedTokenIndex}
          />
        </View>
      )}
      {tokenInfos()}
    </ScreenWrapper>
  );
};

export default WalletHome;
