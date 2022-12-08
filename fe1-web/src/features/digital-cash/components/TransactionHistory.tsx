import { ListItem } from '@rneui/themed';
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { Modal, View } from 'react-native';
import { ScrollView, TouchableWithoutFeedback } from 'react-native-gesture-handler';

import ModalHeader from 'core/components/ModalHeader';
import { Hash } from 'core/objects';
import { List, ModalStyles, Typography } from 'core/styles';
import { COINBASE_HASH } from 'resources/const';
import STRINGS from 'resources/strings';

import { DigitalCashHooks } from '../hooks';
import { Transaction, TransactionState } from '../objects/transaction';

/**
 * UI for the transactions history
 */
const TransactionHistory = ({ laoId }: IPropTypes) => {
  const [showTransactionHistory, setShowTransactionHistory] = useState<boolean>(false);
  const [showInputs, setShowInputs] = useState<boolean>(true);
  const [showOutputs, setShowOutputs] = useState<boolean>(true);

  const [selectedTransaction, setSelectedTransaction] = useState<Transaction | null>(null);
  const [showModal, setShowModal] = useState<boolean>(false);

  const transactions: Transaction[] = DigitalCashHooks.useTransactions(laoId);

  // We need this mapping to show the amount for each input
  const transactionsByHash: Record<string, TransactionState> =
    DigitalCashHooks.useTransactionsByHash(laoId);

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
                    <ListItem.Title style={Typography.base} numberOfLines={1}>
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
                        <ListItem.Title style={Typography.base} numberOfLines={1}>
                          {input.script.publicKey.valueOf()}
                        </ListItem.Title>
                        <ListItem.Subtitle>
                          {input.txOutHash.valueOf() === COINBASE_HASH &&
                            STRINGS.digital_cash_coin_issuance}
                        </ListItem.Subtitle>
                      </ListItem.Content>
                      <ListItem.Title style={Typography.base}>
                        $
                        {input.txOutHash.valueOf() === COINBASE_HASH
                          ? STRINGS.digital_cash_infinity
                          : transactionsByHash[input.txOutHash.valueOf()].outputs[input.txOutIndex]
                              .value}
                      </ListItem.Title>
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
                        <ListItem.Title style={Typography.base} numberOfLines={1}>
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

const propTypes = {
  laoId: PropTypes.instanceOf(Hash).isRequired,
};

TransactionHistory.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default TransactionHistory;
