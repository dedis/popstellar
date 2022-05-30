import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useEffect, useState } from 'react';
import { View } from 'react-native';
import { ListItem } from 'react-native-elements';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { Hash } from 'core/objects';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { WalletHooks } from '../hooks';
import * as Wallet from '../objects';
import { RollCallToken } from '../objects/RollCallToken';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<WalletParamList, typeof STRINGS.navigation_wallet_home>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

/**
 * Wallet UI once the wallet is synced
 */
const WalletHome = () => {
  const rollCallsByLaoId = WalletHooks.useRollCallsByLaoId();
  const namesByLaoId = WalletHooks.useNamesByLaoId();
  const currentLaoId = WalletHooks.useCurrentLaoId();
  const navigation = useNavigation<NavigationProps['navigation']>();

  const [isLaoExpanded, setIsLaoExpanded] = useState<Record<string, boolean | undefined>>({});
  const [tokens, setTokens] = useState<Record<string, RollCallToken[]>>({});

  useEffect(() => {
    if (currentLaoId) {
      setIsLaoExpanded((oldValue) => ({ ...oldValue, [currentLaoId.valueOf()]: true }));
    }
  }, [currentLaoId]);

  useEffect(() => {
    let updateWasCanceled = false;

    // collect an array of promises and wait for all their results
    Promise.all(
      // iterate over all lao ids
      Object.keys(rollCallsByLaoId).map((laoId) =>
        // retrieve all roll call tokens for each lao id
        Wallet.recoverWalletRollCallTokens(rollCallsByLaoId, new Hash(laoId)).then(
          (rollCallTokens) => [laoId, rollCallTokens] as [string, RollCallToken[]],
        ),
      ),
    )
      .then((rollCallTokens) =>
        // combine these arrays into a map from lao id to array of tokens
        rollCallTokens.reduce((obj, [laoId, laoTokens]) => {
          // make reduce more efficient by mutating obkect
          // eslint-disable-next-line no-param-reassign
          obj[laoId] = laoTokens;
          return obj;
        }, {} as Record<string, RollCallToken[]>),
      )
      .then((value) => {
        // then update the state if no new update was triggered
        if (!updateWasCanceled) {
          setTokens(value);
        }
      });

    return () => {
      // cancel update if the hook is called again
      updateWasCanceled = true;
    };
  }, [rollCallsByLaoId]);

  // if we are connected to a lao, then only show data for this lao
  const laoIds = currentLaoId ? [currentLaoId.valueOf()] : Object.keys(rollCallsByLaoId);

  return (
    <ScreenWrapper>
      <View style={List.container}>
        {laoIds.map((laoId) => {
          return (
            <ListItem.Accordion
              containerStyle={List.accordionItem}
              style={List.accordionItem}
              content={
                <ListItem.Content>
                  <ListItem.Title style={[Typography.base, Typography.important]}>
                    {namesByLaoId[laoId]} ({(tokens[laoId] || []).length})
                  </ListItem.Title>
                </ListItem.Content>
              }
              isExpanded={!!isLaoExpanded[laoId]}
              onPress={() =>
                setIsLaoExpanded({ ...isLaoExpanded, [laoId]: !isLaoExpanded[laoId] })
              }>
              {(tokens[laoId] || []).map((rollCall, idx) => {
                const listStyles = List.getListItemStyles(
                  idx === 0,
                  idx === tokens[laoId].length - 1,
                );

                return (
                  <ListItem
                    key={rollCall.rollCallId.valueOf()}
                    containerStyle={listStyles}
                    style={listStyles}
                    onPress={() =>
                      navigation.navigate(STRINGS.navigation_wallet_single_roll_call, {
                        rollCallId: rollCall.rollCallId.valueOf(),
                        rollCallName: rollCall.rollCallName,
                        rollCallTokenPublicKey: rollCall.token.publicKey.valueOf(),
                      })
                    }>
                    <ListItem.Content>
                      <ListItem.Title style={Typography.base}>
                        {rollCall.rollCallName}
                      </ListItem.Title>
                    </ListItem.Content>
                  </ListItem>
                );
              })}
            </ListItem.Accordion>
          );
        })}
      </View>
    </ScreenWrapper>
  );
};

export default WalletHome;
