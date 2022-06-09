import { CompositeScreenProps, useNavigation, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo } from 'react';
import { Text, View } from 'react-native';
import { ListItem } from 'react-native-elements';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import TransactionHistory from '../components/TransactionHistory';
import { DigitalCashHooks } from '../hooks';
import { DigitalCashFeature } from '../interface';
import { RollCallAccount } from '../objects/Account';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<WalletParamList, typeof STRINGS.navigation_wallet_digital_cash_wallet>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

const rollCallAccounts: RollCallAccount[] = [
  {
    rollCallId: 'l1d1c5VwRmz2oiRRjEJh78eEhOnEf8QJ4W5PrmZfxcE=',
    rollCallName: 'a roll call',
    popToken: '-uac6_xEos4Dz8ESBpoAnqLD4vsd3viScjIEcPEQilo=',
    balance: 21.3,
  },
  {
    rollCallId: 'THFll04mCvZxOhCL9DYygnbTBSR2fjQAYGkfTzPf-zc=',
    rollCallName: 'another roll call',
    popToken: '-uac6_xEos4Dz8ESBpoAnqLD4vsd3viScjIEcPEQilo=',
    balance: 20.9,
  },
];

const DigitalCashWallet = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const route = useRoute<NavigationProps['route']>();

  const { laoId } = route.params;

  DigitalCashHooks.useRollCallTokensByLaoId(laoId)
    .then((rollCallTokens) => console.log(rollCallTokens));

  const isOrganizer = DigitalCashHooks.useIsLaoOrganizer(laoId);

  const balance = rollCallAccounts.reduce((sum, account) => sum + (account.balance || 0), 0);

  /**
   * Add coin issuance account for organizers
   */
  const accounts: RollCallAccount[] = useMemo(() => {
    if (isOrganizer) {
      return [
        {
          rollCallName: STRINGS.digital_cash_coin_issuance,
          rollCallId: STRINGS.digital_cash_coin_issuance_description,
          balance: null,
        } as RollCallAccount,
        ...rollCallAccounts,
      ];
    }
    return rollCallAccounts;
  }, [isOrganizer]);

  return (
    <ScreenWrapper>
      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.digital_cash_wallet_balance}: ${balance}
      </Text>
      <Text style={Typography.paragraph}>{STRINGS.digital_cash_wallet_description}</Text>

      <View style={List.container}>
        {accounts.map((account, idx) => {
          const listStyle = List.getListItemStyles(idx === 0, idx === accounts.length - 1);

          return (
            <ListItem
              key={account.rollCallId}
              containerStyle={listStyle}
              style={listStyle}
              bottomDivider
              onPress={() => {
                navigation.navigate(STRINGS.navigation_wallet_digital_cash_send_receive, {
                  laoId,
                  rollCallId: account.rollCallId,
                });
              }}>
              <ListItem.Content>
                <ListItem.Title style={Typography.base}>{account.rollCallName}</ListItem.Title>
                <ListItem.Subtitle
                  style={Typography.small}
                  numberOfLines={account.balance ? 1 : undefined}>
                  {account.rollCallId}
                </ListItem.Subtitle>
              </ListItem.Content>
              <ListItem.Title style={Typography.base}>${account.balance || '∞'}</ListItem.Title>
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
