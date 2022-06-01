import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo, useState } from 'react';
import { Modal, Text, View } from 'react-native';
import { ListItem } from 'react-native-elements';
import { ScrollView, TouchableWithoutFeedback } from 'react-native-gesture-handler';

import { Input, PoPTextButton } from 'core/components';
import ModalHeader from 'core/components/ModalHeader';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { List, ModalStyles, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import TransactionHistory from '../components/TransactionHistory';
import { DigitalCashHooks } from '../hooks';
import { DigitalCashFeature } from '../interface';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<WalletParamList, typeof STRINGS.navigation_wallet_digital_cash_wallet>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

type RollCallAccount = { rollCallId: string; rollCallName: string; balance: null | number };

const DigitalCashWallet = () => {
  const route = useRoute<NavigationProps['route']>();

  const { laoId } = route.params;

  const isOrganizer = DigitalCashHooks.useIsLaoOrganizer(laoId);

  const [selectedAccount, setSelectedAccount] = useState<RollCallAccount | null>(null);
  const [beneficiary, setBeneficiary] = useState('');
  const [amount, setAmount] = useState('');

  const [error, setError] = useState<string | null>(null);

  const [showModal, setShowModal] = useState<boolean>(false);

  const rollCallAccounts: RollCallAccount[] = useMemo(
    () => [
      { rollCallId: 'x', rollCallName: 'a roll call', balance: 21.3 },
      { rollCallId: 'y', rollCallName: 'another roll call', balance: 20.9 },
    ],
    [],
  );
  const balance = rollCallAccounts.reduce((sum, account) => sum + (account.balance || 0), 0);

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
  }, [rollCallAccounts, isOrganizer]);

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
                setSelectedAccount(account);
                setShowModal(true);
              }}>
              <ListItem.Content>
                <ListItem.Title style={Typography.base}>{account.rollCallName}</ListItem.Title>
                <ListItem.Subtitle style={Typography.small}>{account.rollCallId}</ListItem.Subtitle>
              </ListItem.Content>
              <ListItem.Title style={Typography.base}>${account.balance || '∞'}</ListItem.Title>
              <ListItem.Chevron />
            </ListItem>
          );
        })}
      </View>

      <Modal
        transparent
        visible={showModal}
        onRequestClose={() => {
          setShowModal(!showModal);
        }}>
        <TouchableWithoutFeedback
          containerStyle={ModalStyles.modalBackground}
          onPress={() => {
            setShowModal(!showModal);
          }}
        />
        <ScrollView style={ModalStyles.modalContainer}>
          <ModalHeader onClose={() => setShowModal(!showModal)}>
            {STRINGS.digital_cash_wallet_create_transaction}
          </ModalHeader>

          <Text style={[Typography.paragraph, Typography.important]}>
            {STRINGS.digital_cash_wallet_balance}: ${selectedAccount?.balance || '∞'}
          </Text>

          <Text style={Typography.paragraph}>
            {STRINGS.digital_cash_wallet_transaction_description}
          </Text>

          <Text style={[Typography.paragraph, Typography.important]}>
            {STRINGS.digital_cash_wallet_beneficiary}
          </Text>
          <Input
            value={beneficiary}
            onChange={setBeneficiary}
            placeholder={STRINGS.digital_cash_wallet_beneficiary_placeholder}
          />

          <Text style={[Typography.paragraph, Typography.important]}>
            {STRINGS.digital_cash_wallet_amount}
          </Text>
          <Input
            value={amount}
            onChange={setAmount}
            placeholder={STRINGS.digital_cash_wallet_amount_placeholder}
          />

          {error && <Text style={[Typography.paragraph, Typography.error]}>{error}</Text>}
          <PoPTextButton
            disabled={
              Number.isNaN(parseFloat(amount)) ||
              (!!selectedAccount?.balance &&
                selectedAccount &&
                parseFloat(amount) > selectedAccount?.balance)
            }
            onPress={() => {
              if (!selectedAccount) {
                throw new Error(
                  'It should not be possible to send money without selecting an account first',
                );
              }

              const amountToTransfer = Number.parseFloat(amount);
              if (Number.isNaN(amountToTransfer)) {
                setError(STRINGS.digital_cash_wallet_amount_must_be_number);
                return;
              }

              if (selectedAccount.balance && amountToTransfer > selectedAccount.balance) {
                setError(STRINGS.digital_cash_wallet_amount_too_high);
                return;
              }

              setError(null);

              if (selectedAccount.balance) {
                // TODO: transaction
              } else {
                // TODO: coin issuance
              }
            }}>
            {STRINGS.digital_cash_wallet_send_transaction}
          </PoPTextButton>
        </ScrollView>
      </Modal>

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
