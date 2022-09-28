import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import * as Clipboard from 'expo-clipboard';
import React, { useState } from 'react';
import { Modal, ScrollView, StyleSheet, Text, TextStyle, View } from 'react-native';
import { TouchableOpacity, TouchableWithoutFeedback } from 'react-native-gesture-handler';

import { PoPIcon, PoPTextButton, QRCode } from 'core/components';
import ModalHeader from 'core/components/ModalHeader';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { ScannablePopToken } from 'core/objects/ScannablePopToken';
import { Color, Icon, ModalStyles, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<WalletParamList, typeof STRINGS.navigation_wallet_single_roll_call>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

const styles = StyleSheet.create({
  publicKey: {
    marginVertical: Spacing.x2,
    color: Color.inactive,
    textAlign: 'center',
  } as TextStyle,
});

/**
 * Renders the screen for showing a single roll call in the wallet
 */
const WalletSingleRollCall = () => {
  const route = useRoute<NavigationProps['route']>();
  const { rollCallName } = route.params;

  return (
    <ScreenWrapper>
      <Text style={Typography.base}>{STRINGS.wallet_single_roll_call_description}</Text>
      <Text style={Typography.base}>{rollCallName}</Text>
    </ScreenWrapper>
  );
};

export default WalletSingleRollCall;

/**
 * Component shown in the top middle of the navigation bar. Makes sure
 * the name of a roll call is shown instead of some static text
 */
export const ViewSingleRollCallScreenHeader = () => {
  const route = useRoute<NavigationProps['route']>();
  const { rollCallName } = route.params;

  return <Text style={Typography.topNavigationHeading}>{rollCallName}</Text>;
};

/**
 * Component shown in the top right of the navigation bar. Allows the user to interact
 * show the qr code of their pop token.
 */
export const WalletSingleHeaderRight = () => {
  const route = useRoute<NavigationProps['route']>();
  const { rollCallTokenPublicKey } = route.params;

  const [modalVisible, setModalVisible] = useState(false);

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
            <QRCode
              value={ScannablePopToken.encodePopToken({ pop_token: rollCallTokenPublicKey })}
              visibility
            />
          </View>

          <Text style={[Typography.small, styles.publicKey]}>{rollCallTokenPublicKey}</Text>

          <PoPTextButton onPress={() => Clipboard.setStringAsync(rollCallTokenPublicKey)}>
            {STRINGS.wallet_single_roll_call_copy_pop_token}
          </PoPTextButton>
        </ScrollView>
      </Modal>
    </>
  );
};
