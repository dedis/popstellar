import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useState } from 'react';
import { Modal, View } from 'react-native';
import { ListItem } from 'react-native-elements';
import { ScrollView, TouchableWithoutFeedback } from 'react-native-gesture-handler';

import ModalHeader from 'core/components/ModalHeader';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { List, ModalStyles, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { DigitalCashHooks } from '../hooks';
import { Transaction } from '../objects/transaction';
import { COINBASE_HASH } from "../../../resources/const";

type NavigationProps = CompositeScreenProps<
  StackScreenProps<
    WalletParamList,
    typeof STRINGS.navigation_wallet_digital_cash_transaction_history
  >,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

const TransactionHistory = () => {
  const route = useRoute<NavigationProps['route']>();

  const { laoId } = route.params;

  const [showTransactionHistory, setShowTransactionHistory] = useState<boolean>(false);
  const [showInputs, setShowInputs] = useState<boolean>(true);
  const [showOutputs, setShowOutputs] = useState<boolean>(true);

  const [selectedTransaction, setSelectedTransaction] = useState<Transaction | null>(null);
  const [showModal, setShowModal] = useState<boolean>(false);

  const transactions: Transaction[] = DigitalCashHooks.useTransactions(laoId);

  return (
    <>
      <View style={List.container}>
        <ListItem.Accordion
          containerStyle={List.accordionItem}
          style={List.accordionItem}
          content={
            <ListItem.Content>
              <ListItem.Title style={[Typography.base, Typography.important]}>
                {STRINGS.digital_cash_wallet_transaction_history}
              </ListItem.Title>
            </ListItem.Content>
          }
          isExpanded={showTransactionHistory}
          onPress={() => setShowTransactionHistory(!showTransactionHistory)}>
          {transactions
            .map((transaction, idx) => {
              const listStyle = List.getListItemStyles(idx === transactions.length - 1, idx === 0);

              const amount = transaction.outputs.reduce((sum, output) => sum + output.value, 0);

              return (
                <ListItem
                  key={transaction.transactionId.valueOf()}
                  containerStyle={listStyle}
                  style={listStyle}
                  bottomDivider
                  onPress={() => {
                    setSelectedTransaction(transaction);
                    setShowInputs(true);
                    setShowOutputs(true);
                    setShowModal(true);
                  }}>
                  <ListItem.Content>
                    <ListItem.Title style={Typography.base}>
                      {transaction.transactionId.valueOf()}
                    </ListItem.Title>
                    <ListItem.Subtitle style={Typography.small}>
                      {STRINGS.digital_cash_wallet_transaction_inputs}: {transaction.inputs.length},{' '}
                      {STRINGS.digital_cash_wallet_transaction_outputs}:{' '}
                      {transaction.outputs.length}
                    </ListItem.Subtitle>
                  </ListItem.Content>
                  <ListItem.Title style={Typography.base}>${amount}</ListItem.Title>
                  <ListItem.Chevron />
                </ListItem>
              );
            })
            .reverse()}
        </ListItem.Accordion>
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
            {STRINGS.digital_cash_wallet_transaction}
          </ModalHeader>

          <View style={List.container}>
            {/* inputs */}
            <ListItem.Accordion
              containerStyle={List.accordionItem}
              style={List.accordionItem}
              content={
                <ListItem.Content>
                  <ListItem.Title style={[Typography.base, Typography.important]}>
                    {STRINGS.digital_cash_wallet_transaction_inputs} (
                    {selectedTransaction?.inputs.length})
                  </ListItem.Title>
                </ListItem.Content>
              }
              isExpanded={showInputs}
              onPress={() => setShowInputs(!showInputs)}>
              {selectedTransaction &&
                selectedTransaction.inputs.map((input, idx) => {
                  const listStyle = List.getListItemStyles(
                    idx === 0,
                    idx === selectedTransaction.inputs.length - 1,
                  );

                  return (
                    <ListItem
                      key={input.txOutHash.valueOf().concat(input.txOutIndex.toString(10))}
                      containerStyle={listStyle}
                      style={listStyle}
                      bottomDivider>
                      <ListItem.Content>
                        <ListItem.Title style={Typography.base}>
                          {input.script.publicKey.valueOf()}
                        </ListItem.Title>
                        <ListItem.Subtitle>
                          {input.txOutHash.valueOf() === COINBASE_HASH &&
                            STRINGS.digital_cash_coin_issuance}
                        </ListItem.Subtitle>
                      </ListItem.Content>
                      <ListItem.Title style={Typography.base}>${1087479784}</ListItem.Title>
                    </ListItem>
                  );
                })}
            </ListItem.Accordion>
            {/* outputs */}
            <ListItem.Accordion
              containerStyle={List.accordionItem}
              style={List.accordionItem}
              content={
                <ListItem.Content>
                  <ListItem.Title style={[Typography.base, Typography.important]}>
                    {STRINGS.digital_cash_wallet_transaction_outputs} (
                    {selectedTransaction?.outputs.length})
                  </ListItem.Title>
                </ListItem.Content>
              }
              isExpanded={showOutputs}
              onPress={() => setShowOutputs(!showOutputs)}>
              {selectedTransaction &&
                selectedTransaction.outputs.map((output, idx) => {
                  const listStyle = List.getListItemStyles(
                    idx === 0,
                    idx === selectedTransaction.outputs.length - 1,
                  );

                  return (
                    <ListItem
                      key={output.script.publicKeyHash.valueOf()}
                      containerStyle={listStyle}
                      style={listStyle}
                      bottomDivider>
                      <ListItem.Content>
                        <ListItem.Title style={Typography.base}>
                          {output.script.publicKeyHash.valueOf()}
                        </ListItem.Title>
                      </ListItem.Content>
                      <ListItem.Title style={Typography.base}>${output.value}</ListItem.Title>
                    </ListItem>
                  );
                })}
            </ListItem.Accordion>
          </View>
        </ScrollView>
      </Modal>
    </>
  );
};

export default TransactionHistory;
