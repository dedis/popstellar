import { CompositeScreenProps, useNavigation, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useEffect, useReducer, useState } from 'react';
import { Modal, StyleSheet, Text, TextStyle, View } from 'react-native';
import {
  ScrollView,
  TouchableOpacity,
  TouchableWithoutFeedback,
} from 'react-native-gesture-handler';

import { Input, PoPIcon, PoPTextButton, QRCode } from 'core/components';
import ModalHeader from 'core/components/ModalHeader';
import ScannerInput from 'core/components/ScannerInput';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { getNavigator } from 'core/platform/Navigator';
import { Color, Icon, ModalStyles, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { DigitalCashFeature } from '../../interface';
import { RollCallAccount } from '../../objects/Account';
import { SendReciveStateActionType, digitalCashWalletStateReducer } from './SendReceiveState';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<WalletParamList, typeof STRINGS.navigation_wallet_digital_cash_send_receive>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

const styles = StyleSheet.create({
  publicKey: {
    marginVertical: Spacing.x2,
    color: Color.inactive,
    textAlign: 'center',
  } as TextStyle,
});

// the account should be retrieved from the redux store using the roll
// call id provdied in the route parameters
const account: RollCallAccount = {
  rollCallId: 'l1d1c5VwRmz2oiRRjEJh78eEhOnEf8QJ4W5PrmZfxcE=',
  rollCallName: 'a roll call',
  popToken: '-uac6_xEos4Dz8ESBpoAnqLD4vsd3viScjIEcPEQilo=',
  balance: 21.3,
};

const SendReceive = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const route = useRoute<NavigationProps['route']>();

  const { laoId, scannedPoPToken, scannedPoPTokenBeneficiaryIndex } = route.params;

  /**
   * The component state. This is a react reducer, similar to redux reducers and allows
   * us to hide the complex state update logic from the UI code
   */
  const [{ beneficiaries, error }, dispatch] = useReducer(digitalCashWalletStateReducer, {
    beneficiaries: [{ amount: '', popToken: '' }],
    error: null,
  });

  const totalAmount = beneficiaries.reduce((sum, target) => sum + parseFloat(target.amount), 0);

  const onSendTransaction = () => {
    if (!account) {
      throw new Error('It should not be possible to send money without selecting an account first');
    }

    if (Number.isNaN(totalAmount)) {
      dispatch({
        type: SendReciveStateActionType.SET_ERROR,
        error: STRINGS.digital_cash_wallet_amount_must_be_number,
      });
      return;
    }

    if (account.balance && totalAmount > account.balance) {
      dispatch({
        type: SendReciveStateActionType.SET_ERROR,
        error: STRINGS.digital_cash_wallet_amount_too_high,
      });
      return;
    }

    dispatch({
      type: SendReciveStateActionType.CLEAR_ERROR,
    });

    if (account.balance) {
      // TODO: transaction
    } else {
      // TODO: coin issuance
    }
  };

  const cannotSendTransaction =
    Number.isNaN(totalAmount) || (!!account?.balance && account && totalAmount > account?.balance);

  useEffect(() => {
    if (
      scannedPoPTokenBeneficiaryIndex !== undefined &&
      scannedPoPTokenBeneficiaryIndex < beneficiaries.length &&
      scannedPoPToken
    ) {
      dispatch({
        type: SendReciveStateActionType.INSERT_SCANNED_POP_TOKEN,
        beneficiaryIndex: scannedPoPTokenBeneficiaryIndex,
        beneficiaryPopToken: scannedPoPToken,
      });
    }
    // should only be re-executed of the navigation parameters change
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [scannedPoPToken, scannedPoPTokenBeneficiaryIndex]);

  return (
    <ScreenWrapper>
      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.digital_cash_wallet_balance}: ${account?.balance || '∞'}
      </Text>

      <Text style={Typography.paragraph}>
        {STRINGS.digital_cash_wallet_transaction_description}
      </Text>

      {beneficiaries.map(({ amount, popToken }, index) => (
        <View key={index.toString()}>
          <Text style={[Typography.paragraph, Typography.important]}>
            {STRINGS.digital_cash_wallet_beneficiary}
          </Text>
          <ScannerInput
            value={popToken}
            onChange={(newPopToken) =>
              dispatch({
                type: SendReciveStateActionType.UPDATE_BENEFICIARY,
                beneficiaryIndex: index,
                popToken: newPopToken,
              })
            }
            onPress={() => {
              if (!account) {
                throw new Error(
                  'It should not be possible to get here without selecting an account first',
                );
              }

              navigation.navigate(STRINGS.navigation_wallet_digital_cash_wallet_scanner, {
                laoId: laoId.valueOf(),
                rollCallId: account.rollCallId,
                beneficiaryIndex: index,
              });
            }}
            placeholder={STRINGS.digital_cash_wallet_beneficiary_placeholder}
          />

          <Text style={[Typography.paragraph, Typography.important]}>
            {STRINGS.digital_cash_wallet_amount}
          </Text>
          <Input
            value={amount}
            onChange={(newAmount) =>
              dispatch({
                type: SendReciveStateActionType.UPDATE_BENEFICIARY,
                beneficiaryIndex: index,
                amount: newAmount,
              })
            }
            placeholder={STRINGS.digital_cash_wallet_amount_placeholder}
          />
        </View>
      ))}

      {error && <Text style={[Typography.paragraph, Typography.error]}>{error}</Text>}
      <PoPTextButton onPress={() => dispatch({ type: SendReciveStateActionType.ADD_BENEFICIARY })}>
        {STRINGS.digital_cash_wallet_add_beneficiary}
      </PoPTextButton>

      <PoPTextButton disabled={cannotSendTransaction} onPress={onSendTransaction}>
        {STRINGS.digital_cash_wallet_send_transaction}
      </PoPTextButton>
    </ScreenWrapper>
  );
};

export default SendReceive;

/**
 * Component shown in the top right of the navigation bar. Allows the user to interact
 * show the qr code of their pop token in order to receive money
 */
export const SendReceiveHeaderRight = () => {
  const [modalVisible, setModalVisible] = useState(false);

  if (!account.balance) {
    return null;
  }

  return (
    <>
      <TouchableOpacity onPress={() => setModalVisible(!modalVisible)}>
        <PoPIcon name="qrCode" color={Color.inactive} size={Icon.size} />
      </TouchableOpacity>

      <Modal
        transparent
        visible={modalVisible}
        onRequestClose={() => {
          setModalVisible(!modalVisible);
        }}>
        <TouchableWithoutFeedback
          containerStyle={ModalStyles.modalBackground}
          onPress={() => {
            setModalVisible(!modalVisible);
          }}
        />
        <ScrollView style={ModalStyles.modalContainer}>
          <ModalHeader onClose={() => setModalVisible(!modalVisible)}>
            {STRINGS.wallet_single_roll_call_pop_token}
          </ModalHeader>

          <View>
            <QRCode value={account.popToken} visibility />
          </View>

          <Text style={[Typography.small, styles.publicKey]}>{account.popToken}</Text>

          <PoPTextButton onPress={() => getNavigator().clipboard.writeText(account.popToken)}>
            {STRINGS.wallet_single_roll_call_copy_pop_token}
          </PoPTextButton>
        </ScrollView>
      </Modal>
    </>
  );
};

export const SendReceiveScreen: DigitalCashFeature.WalletScreen = {
  id: STRINGS.navigation_wallet_digital_cash_send_receive,
  title: STRINGS.navigation_wallet_digital_cash_send_receive_title,
  Component: SendReceive,
  headerRight: SendReceiveHeaderRight,
};
