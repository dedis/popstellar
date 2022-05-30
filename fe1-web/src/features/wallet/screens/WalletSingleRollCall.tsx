import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useState } from 'react';
import { Modal, ScrollView, StyleSheet, Text, TextStyle, View } from 'react-native';
import { TouchableOpacity, TouchableWithoutFeedback } from 'react-native-gesture-handler';

import { Button, QRCode } from 'core/components';
import QrCodeIcon from 'core/components/icons/QrCodeIcon';
import ModalHeader from 'core/components/ModalHeader';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { getNavigator } from 'core/platform/Navigator';
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

export const ViewSingleRollCallScreenHeader = () => {
  const route = useRoute<NavigationProps['route']>();
  const { rollCallName } = route.params;

  return <Text style={Typography.topNavigationHeading}>{rollCallName}</Text>;
};

export const WalletSingleHeaderRight = () => {
  const route = useRoute<NavigationProps['route']>();
  const { rollCallTokenPublicKey } = route.params;

  const [modalVisible, setModalVisible] = useState(false);

  return (
    <>
      <TouchableOpacity onPress={() => setModalVisible(!modalVisible)}>
        <QrCodeIcon color={Color.inactive} size={Icon.size} />
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
            <QRCode value={rollCallTokenPublicKey} visibility />
          </View>

          <Text style={[Typography.small, styles.publicKey]}>{rollCallTokenPublicKey}</Text>

          <Button onPress={() => getNavigator().clipboard.writeText(rollCallTokenPublicKey)}>
            <Text style={[Typography.base, Typography.centered, Typography.negative]}>
              {STRINGS.wallet_single_roll_call_copy_pop_token}
            </Text>
          </Button>
        </ScrollView>
      </Modal>
    </>
  );
};
