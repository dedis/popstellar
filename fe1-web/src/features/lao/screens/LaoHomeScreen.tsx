import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useState } from 'react';
import { Modal, StyleSheet, Text, View, ViewStyle } from 'react-native';
import { ScrollView, TouchableWithoutFeedback } from 'react-native-gesture-handler';

import { PoPIcon, QRCode } from 'core/components';
import ModalHeader from 'core/components/ModalHeader';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { getNetworkManager } from 'core/network';
import { Color, Icon, ModalStyles, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { LaoProperties } from '../components';
import Identity from '../components/Identity';
import { LaoHooks } from '../hooks';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_home>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

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
});

/**
 * Component rendered in the top middle of the navgiation bar when looking
 * at the lao home screen. Makes sure it shows the name of the lao and
 * not just some static string.
 */
export const LaoHomeScreenHeader = () => {
  const lao = LaoHooks.useCurrentLao();
  const [modalVisible, setModalVisible] = useState(false);

  return (
    <>
      <PoPTouchableOpacity onPress={() => setModalVisible(!modalVisible)} style={styles.header}>
        <Text style={Typography.topNavigationHeading}>{lao.name}</Text>
        <View style={styles.infoIcon}>
          <PoPIcon name="info" color={Color.primary} size={Icon.size} />
        </View>
      </PoPTouchableOpacity>

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

/**
 * Component rendered in the top left of the navigation bar
 * Shows a disconnect icon for disconnecting from the lao
 */
export const LaoHomeScreenHeaderLeft = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();

  return (
    <>
      <PoPTouchableOpacity
        onPress={() => {
          getNetworkManager().disconnectFromAll();

          navigation.navigate(STRINGS.navigation_app_home, {
            screen: STRINGS.navigation_home_home,
          });
        }}>
        <PoPIcon name="arrowBack" color={Color.inactive} size={Icon.size} />
      </PoPTouchableOpacity>
    </>
  );
};

/**
 * Component rendered in the top right of the navigation bar
 * Shows a qr code icon for showing the lao connection qr code
 * and a bell icon for accessing the notifications menu
 */
export const LaoHomeScreenHeaderRight = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();

  const [modalVisible, setModalVisible] = useState(false);

  const lao = LaoHooks.useCurrentLao();
  const encodeLaoConnection = LaoHooks.useEncodeLaoConnectionForQRCode();

  return (
    <>
      <View style={styles.buttons}>
        <PoPTouchableOpacity onPress={() => setModalVisible(!modalVisible)}>
          <PoPIcon name="qrCode" color={Color.inactive} size={Icon.size} />
        </PoPTouchableOpacity>
        <PoPTouchableOpacity
          containerStyle={styles.notificationButton}
          onPress={() =>
            navigation.push(STRINGS.navigation_app_lao, {
              screen: STRINGS.navigation_lao_notifications,
              params: {
                screen: STRINGS.navigation_notification_notifications,
              },
            })
          }>
          <PoPIcon name="notification" color={Color.inactive} size={Icon.size} />
        </PoPTouchableOpacity>

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

            <View>
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
