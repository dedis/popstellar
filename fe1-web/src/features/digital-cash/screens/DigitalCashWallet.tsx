import { CompositeScreenProps, useNavigation, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useEffect, useMemo, useState } from 'react';
import { Text, View } from 'react-native';
import { ListItem } from 'react-native-elements';
import { useSelector } from 'react-redux';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { Hash } from 'core/objects';
import { RollCallToken } from 'core/objects/RollCallToken';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import TransactionHistory from '../components/TransactionHistory';
import { DigitalCashHooks } from '../hooks';
import { DigitalCashFeature } from '../interface';
import { makeBalancesSelector } from '../reducer/DigitalCashReducer';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<WalletParamList, typeof STRINGS.navigation_wallet_digital_cash_wallet>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

const DigitalCashWallet = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const route = useRoute<NavigationProps['route']>();
  const [rollCallTokens, setRollCallTokens] = useState<RollCallToken[]>([]);

  const { laoId } = route.params;

  const balances = useSelector(useMemo(() => makeBalancesSelector(laoId), [laoId]));

  const rollCallTokensFetcher = DigitalCashHooks.useRollCallTokensByLaoId(laoId);

  useEffect(() => {
    rollCallTokensFetcher.then(setRollCallTokens);
  }, [rollCallTokensFetcher]);

  const balance = rollCallTokens.reduce(
    (sum, account) => sum + (balances[Hash.fromPublicKey(account.token.publicKey).valueOf()] || 0),
    0,
  );

  return (
    <ScreenWrapper>
      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.digital_cash_wallet_balance}: ${balance}
      </Text>
      <Text style={Typography.paragraph}>{STRINGS.digital_cash_wallet_description}</Text>

      <View style={List.container}>
        {rollCallTokens.map((rollCallToken, idx) => {
          const listStyle = List.getListItemStyles(idx === 0, idx === rollCallTokens.length - 1);

          return (
            <ListItem
              key={rollCallToken.rollCallId.valueOf()}
              containerStyle={listStyle}
              style={listStyle}
              bottomDivider
              onPress={() => {
                navigation.navigate(STRINGS.navigation_wallet_digital_cash_send_receive, {
                  laoId,
                  rollCallId: rollCallToken.rollCallId.valueOf(),
                });
              }}>
              <ListItem.Content>
                <ListItem.Title style={Typography.base}>
                  {rollCallToken.rollCallName}
                </ListItem.Title>
                <ListItem.Subtitle style={Typography.small} numberOfLines={undefined}>
                  {rollCallToken.rollCallId.valueOf()}
                </ListItem.Subtitle>
              </ListItem.Content>
              <ListItem.Title style={Typography.base}>
                ${balances[Hash.fromPublicKey(rollCallToken.token.publicKey).valueOf()] || 0}
              </ListItem.Title>
              <ListItem.Chevron />
            </ListItem>
          );
        })}
      </View>

      <TransactionHistory />
    </ScreenWrapper>
  );
};

export default DigitalCashWallet;

export const DigitalCashWalletScreen: DigitalCashFeature.WalletScreen = {
  id: STRINGS.navigation_wallet_digital_cash_wallet,
  title: STRINGS.digital_cash_wallet_screen_title,
  Component: DigitalCashWallet,
};
