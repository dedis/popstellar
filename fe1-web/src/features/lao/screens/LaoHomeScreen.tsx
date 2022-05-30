import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useState } from 'react';
import { Modal, StyleSheet, Text, View, ViewStyle } from 'react-native';
import {
  ScrollView,
  TouchableOpacity,
  TouchableWithoutFeedback,
} from 'react-native-gesture-handler';

import { QRCode } from 'core/components';
import InfoIcon from 'core/components/icons/InfoIcon';
import NotificationIcon from 'core/components/icons/NotificationIcon';
import QrCodeIcon from 'core/components/icons/QrCodeIcon';
import ModalHeader from 'core/components/ModalHeader';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Color, Icon, ModalStyles, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { LaoProperties } from '../components';
import Identity from '../components/Identity';
import { LaoHooks } from '../hooks';

const LaoHomeScreen = () => {
  return (
    <ScreenWrapper>
      <Identity />
    </ScreenWrapper>
  );
};

export default LaoHomeScreen;

const styles = StyleSheet.create({
  header: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  } as ViewStyle,
  infoIcon: {
    marginLeft: Spacing.x025,
  },
  buttons: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  },
  notificationButton: {
    marginLeft: Spacing.x1,
  },
  qrCodeContainer: {
    marginTop: Spacing.x2,
    marginBottom: Spacing.x1,
  },
});

export const LaoHomeScreenHeader = () => {
  const lao = LaoHooks.useCurrentLao();
  const [modalVisible, setModalVisible] = useState(false);

  return (
    <>
      <TouchableOpacity onPress={() => setModalVisible(!modalVisible)} style={styles.header}>
        <Text style={Typography.topNavigationHeading}>{lao.name}</Text>
        <View style={styles.infoIcon}>
          <InfoIcon color={Color.primary} size={Icon.size} />
        </View>
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
          <ModalHeader onClose={() => setModalVisible(!modalVisible)}>{lao.name}</ModalHeader>
          <LaoProperties />
        </ScrollView>
      </Modal>
    </>
  );
};

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_home>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

export const LaoHomeScreenHeaderRight = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();

  const [modalVisible, setModalVisible] = useState(false);

  const lao = LaoHooks.useCurrentLao();
  const encodeLaoConnection = LaoHooks.useEncodeLaoConnectionForQRCode();

  return (
    <>
      <View style={styles.buttons}>
        <TouchableOpacity onPress={() => setModalVisible(!modalVisible)}>
          <QrCodeIcon color={Color.inactive} size={Icon.size} />
        </TouchableOpacity>
        <TouchableOpacity
          containerStyle={styles.notificationButton}
          onPress={() =>
            navigation.push(STRINGS.navigation_app_lao, {
              screen: STRINGS.navigation_lao_notifications,
              params: {
                screen: STRINGS.navigation_notification_notifications,
              },
            })
          }>
          <NotificationIcon color={Color.inactive} size={Icon.size} />
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
              {STRINGS.lao_qr_code_title}
            </ModalHeader>

            <View style={styles.qrCodeContainer}>
              <QRCode
                value={encodeLaoConnection(lao.server_addresses, lao.id.toString())}
                visibility
              />
            </View>
          </ScrollView>
        </Modal>
      </View>
    </>
  );
};
