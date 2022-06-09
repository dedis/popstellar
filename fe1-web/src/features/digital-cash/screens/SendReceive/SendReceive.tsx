import { CompositeScreenProps, useNavigation, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useEffect, useMemo, useState } from 'react';
import { Modal, StyleSheet, Text, TextStyle, View } from 'react-native';
import {
  ScrollView,
  TouchableOpacity,
  TouchableWithoutFeedback,
} from 'react-native-gesture-handler';
import { useSelector } from 'react-redux';

import { Input, PoPIcon, PoPTextButton, QRCode } from 'core/components';
import ModalHeader from 'core/components/ModalHeader';
import ScannerInput from 'core/components/ScannerInput';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { getNavigator } from 'core/platform/Navigator';
import { Color, Icon, ModalStyles, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { PublicKey } from '../../../../core/objects';
import { RollCallToken } from '../../../../core/objects/RollCallToken';
import { DigitalCashHooks } from '../../hooks';
import { DigitalCashFeature } from '../../interface';
import { requestSendTransaction } from '../../network';
import { makeBalanceSelector } from '../../reducer/DigitalCashReducer';

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

const SendReceive = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const route = useRoute<NavigationProps['route']>();

  const [rollCallToken, setRollCallToken] = useState<RollCallToken>();

  const [beneficiary, setBeneficiary] = useState('');
  const [amount, setAmount] = useState('');
  const [error, setError] = useState('');

  const { laoId, rollCallId, scannedPoPToken } = route.params;

  DigitalCashHooks.useRollCallTokenByRollCallId(laoId, rollCallId).then(setRollCallToken);

  const balanceSelector = useMemo(() => {
    if (rollCallToken) {
      return makeBalanceSelector(laoId, rollCallToken.token.publicKey.valueOf());
    }
    return () => 0;
  }, [rollCallToken, laoId]);

  const balance = useSelector(balanceSelector);

  useEffect(() => {
    if (scannedPoPToken) {
      setBeneficiary(scannedPoPToken);
    }
  }, [scannedPoPToken]);

  const onSendTransaction = () => {
    if (!rollCallToken) {
      throw new Error('The roll call token is not defined');
    }
    if (beneficiary === '') {
      setError('Euuu');
      return;
    }
    requestSendTransaction(
      rollCallToken.token,
      new PublicKey(beneficiary),
      Number.parseInt(amount, 10),
      rollCallToken.laoId,
    ).then(
      () => {
        console.log('Transaction sent');
      },
      () => {
        console.log('Transaction failed');
      },
    );
  };

  const cannotSendTransaction =
    Number.isNaN(amount) || balance < Number.parseInt(amount, 10) || amount === '';

  return (
    <ScreenWrapper>
      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.digital_cash_wallet_balance}: ${balance}
      </Text>

      <Text style={Typography.paragraph}>
        {STRINGS.digital_cash_wallet_transaction_description}
      </Text>
      <View>
        <Text style={[Typography.paragraph, Typography.important]}>
          {STRINGS.digital_cash_wallet_beneficiary}
        </Text>
        <ScannerInput
          value={beneficiary}
          onChange={setBeneficiary}
          onPress={() => {
            navigation.navigate(STRINGS.navigation_wallet_digital_cash_wallet_scanner, {
              laoId: laoId.valueOf(),
              rollCallId: rollCallId,
            });
          }}
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
      </View>
      {error !== '' && <Text style={[Typography.paragraph, Typography.error]}>{error}</Text>}

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

  const account = {
    popToken: 'iuztu',
  };

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
