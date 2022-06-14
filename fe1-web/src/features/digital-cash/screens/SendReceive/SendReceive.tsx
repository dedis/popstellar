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

import { DropdownSelector, Input, PoPIcon, PoPTextButton, QRCode } from 'core/components';
import ModalHeader from 'core/components/ModalHeader';
import ScannerInput from 'core/components/ScannerInput';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { KeyPairStore } from 'core/keypair';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { Hash, PublicKey } from 'core/objects';
import { getNavigator } from 'core/platform/Navigator';
import { Color, Icon, ModalStyles, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { DigitalCashHooks } from '../../hooks';
import { DigitalCashFeature } from '../../interface';
import { requestCoinbaseTransaction, requestSendTransaction } from '../../network';
import { makeBalanceSelector } from '../../reducer';

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

  const { laoId, rollCallId, isCoinbase, scannedPoPToken } = route.params;

  // will be undefined for the organizer
  const rollCallToken = DigitalCashHooks.useRollCallTokenByRollCallId(laoId, rollCallId || '');

  const allRollCalls = DigitalCashHooks.useRollCallsByLaoId(laoId);

  // will always be '' in non-coinbase transactions, indicates a single beneficiary
  const [selectedRollCallId, setSelectedRollCallId] = useState<string>('');
  const selectedRollCall = DigitalCashHooks.useRollCallById(selectedRollCallId || '');

  const [beneficiary, setBeneficiary] = useState('');
  const [amount, setAmount] = useState('');
  const [error, setError] = useState('');

  if (!(rollCallId || isCoinbase)) {
    throw new Error(
      'The source of a transaction must either be a roll call token or it must be a coinbase transaction',
    );
  }

  const balanceSelector = useMemo(() => {
    if (isCoinbase) {
      return () => Number.POSITIVE_INFINITY;
    }

    if (rollCallToken) {
      return makeBalanceSelector(laoId, rollCallToken.token.publicKey.valueOf());
    }

    return () => 0;
  }, [isCoinbase, rollCallToken, laoId]);

  const balance = useSelector(balanceSelector);

  useEffect(() => {
    if (scannedPoPToken) {
      setBeneficiary(scannedPoPToken);
    }
  }, [scannedPoPToken]);

  const checkBeneficiaryValidity = (): boolean => {
    if (!isCoinbase && beneficiary === '') {
      setError(STRINGS.digital_cash_wallet_add_beneficiary);
      return false;
    }
    return true;
  };

  const checkAmountValidity = (): boolean => {
    const intAmount = Number.parseInt(amount, 10);

    if (Number.isNaN(intAmount) || intAmount < 0) {
      setError(STRINGS.digital_cash_wallet_amount_must_be_number);
      return false;
    }

    if (!isCoinbase && intAmount > balance) {
      setError(STRINGS.digital_cash_wallet_amount_too_high);
      return false;
    }

    if (Number.parseInt(amount, 10) !== Number.parseFloat(amount)){
      setError(STRINGS.digital_cash_wallet_amount_must_be_integer);
      return false;
    }
    return true;
  };

  const sendCoinbaseTransaction = () => {
    let beneficiaries: PublicKey[] = [];

    if (selectedRollCallId !== '') {
      if (!selectedRollCall) {
        throw new Error(
          'Something went terribly wrong, an invalid roll call id could be selected by the user!',
        );
      }
      beneficiaries = selectedRollCall.attendees || [];
    } else {
      beneficiaries = [new PublicKey(beneficiary)];
    }

    return requestCoinbaseTransaction(
      KeyPairStore.get(),
      beneficiaries,
      Number.parseInt(amount, 10),
      new Hash(laoId),
    );
  };

  const sendTransaction = () => {
    if (!rollCallToken) {
      throw new Error('The roll call token is not defined');
    }

    return requestSendTransaction(
      rollCallToken.token,
      new PublicKey(beneficiary),
      Number.parseInt(amount, 10),
      rollCallToken.laoId,
    );
  };
  const onSendTransaction = () => {
    if (checkAmountValidity() && checkBeneficiaryValidity()) {
      const transactionPromise: Promise<void> = isCoinbase ? sendCoinbaseTransaction() : sendTransaction();

      transactionPromise
        .then(() => {
          navigation.goBack();
          console.log('Coinbase transaction sent');
        })
        .catch((reason) => {
          const errorMessage = 'toString' in reason ? reason.toString() : 'Unknown error';

          const err = `Coinbase transaction failed: ${errorMessage}`;
          setError(err);
          console.log(err);
        });
    }
  };

  const cannotSendTransaction =
    Number.isNaN(amount) || (!isCoinbase && balance < Number.parseInt(amount, 10)) || amount === '';

  return (
    <ScreenWrapper>
      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.digital_cash_wallet_balance}: $
        {Number.isFinite(balance) ? balance : STRINGS.digital_cash_infinity}
      </Text>
      <Text style={Typography.paragraph}>
        {STRINGS.digital_cash_wallet_transaction_description}
      </Text>
      <View>
        <Text style={[Typography.paragraph, Typography.important]}>
          {STRINGS.digital_cash_wallet_beneficiary}
        </Text>
        {isCoinbase && (
          <DropdownSelector
            selected={selectedRollCallId}
            onChange={(value) => {
              if (value !== null) {
                setSelectedRollCallId(value);
              }
            }}
            options={[
              {
                value: '',
                label: STRINGS.digital_cash_wallet_issue_single_beneficiary,
              },
              ...Object.entries(allRollCalls).map(([rcId, rc]) => ({
                value: rcId,
                label: `${STRINGS.digital_cash_wallet_issue_all_attendees} "${rc.name}"`,
              })),
            ]}
          />
        )}
        {selectedRollCallId === '' && (
          <ScannerInput
            value={beneficiary}
            onChange={setBeneficiary}
            onPress={() => {
              navigation.navigate(STRINGS.navigation_wallet_digital_cash_wallet_scanner, {
                laoId: laoId.valueOf(),
                rollCallId: rollCallId,
                isCoinbase: isCoinbase,
              });
            }}
            placeholder={STRINGS.digital_cash_wallet_beneficiary_placeholder}
          />
        )}
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

  const route = useRoute<NavigationProps['route']>();

  const { laoId, rollCallId, isCoinbase } = route.params;

  const rollCallToken = DigitalCashHooks.useRollCallTokenByRollCallId(laoId, rollCallId || '');

  const publicKey = useMemo(() => rollCallToken?.token.publicKey.valueOf() || '', [rollCallToken]);

  if (isCoinbase) {
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
            <QRCode value={publicKey} visibility />
          </View>

          <Text style={[Typography.small, styles.publicKey]}>{publicKey}</Text>
          <PoPTextButton onPress={() => getNavigator().clipboard.writeText(publicKey)}>
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
