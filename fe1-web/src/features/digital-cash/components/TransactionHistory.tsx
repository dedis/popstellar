import React, { useState } from 'react';
import { Modal, View } from 'react-native';
import { ListItem } from 'react-native-elements';
import { ScrollView, TouchableWithoutFeedback } from 'react-native-gesture-handler';

import ModalHeader from 'core/components/ModalHeader';
import { List, ModalStyles, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

type Transaction = {
  id: string;
  from: { address: string; amount: number }[];
  to: { address: string; amount: number }[];
};

const TransactionHistory = () => {
  const [showTransactionHistory, setShowTransactionHistory] = useState<boolean>(false);
  const [showInputs, setShowInputs] = useState<boolean>(true);
  const [showOutputs, setShowOutputs] = useState<boolean>(true);

  const [selectedTransaction, setSelectedTransaction] = useState<Transaction | null>(null);
  const [showModal, setShowModal] = useState<boolean>(false);

  const transactions: Transaction[] = [
    {
      id: '0',
      from: [
        { address: 'x', amount: 1 },
        { address: 'y', amount: 2 },
      ],
      to: [
        { address: 'a', amount: 1.5 },
        { address: 'b', amount: 1.5 },
      ],
    },
    {
      id: '1',
      from: [{ address: 'a', amount: 1.5 }],
      to: [{ address: 'b', amount: 1.5 }],
    },
  ];

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
          {transactions.map((transaction, idx) => {
            const listStyle = List.getListItemStyles(idx === 0, idx === transactions.length - 1);

            const amount = transaction.from.reduce((sum, input) => sum + input.amount, 0);

            return (
              <ListItem
                key={transaction.id}
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
                  <ListItem.Title style={Typography.base}>{transaction.id}</ListItem.Title>
                  <ListItem.Subtitle style={Typography.small}>
                    {STRINGS.digital_cash_wallet_transaction_inputs}: {transaction.from.length},{' '}
                    {STRINGS.digital_cash_wallet_transaction_outputs}: {transaction.to.length}
                  </ListItem.Subtitle>
                </ListItem.Content>
                <ListItem.Title style={Typography.base}>${amount}</ListItem.Title>
                <ListItem.Chevron />
              </ListItem>
            );
          })}
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
                    {selectedTransaction?.from.length})
                  </ListItem.Title>
                </ListItem.Content>
              }
              isExpanded={showInputs}
              onPress={() => setShowInputs(!showInputs)}>
              {selectedTransaction &&
                selectedTransaction.from &&
                selectedTransaction.from.map((input, idx) => {
                  const listStyle = List.getListItemStyles(
                    idx === 0,
                    idx === selectedTransaction.from.length - 1,
                  );

                  return (
                    <ListItem
                      key={input.address}
                      containerStyle={listStyle}
                      style={listStyle}
                      bottomDivider>
                      <ListItem.Content>
                        <ListItem.Title style={Typography.base}>{input.address}</ListItem.Title>
                      </ListItem.Content>
                      <ListItem.Title style={Typography.base}>${input.amount}</ListItem.Title>
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
                    {selectedTransaction?.to.length})
                  </ListItem.Title>
                </ListItem.Content>
              }
              isExpanded={showOutputs}
              onPress={() => setShowOutputs(!showOutputs)}>
              {selectedTransaction &&
                selectedTransaction.to &&
                selectedTransaction.to.map((input, idx) => {
                  const listStyle = List.getListItemStyles(
                    idx === 0,
                    idx === selectedTransaction.to.length - 1,
                  );

                  return (
                    <ListItem
                      key={input.address}
                      containerStyle={listStyle}
                      style={listStyle}
                      bottomDivider>
                      <ListItem.Content>
                        <ListItem.Title style={Typography.base}>{input.address}</ListItem.Title>
                      </ListItem.Content>
                      <ListItem.Title style={Typography.base}>${input.amount}</ListItem.Title>
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
