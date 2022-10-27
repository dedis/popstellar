import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useState } from 'react';
import { Modal, StyleSheet, Text, View, ViewStyle } from 'react-native';
import { ScrollView, TouchableWithoutFeedback } from 'react-native-gesture-handler';
import { useSelector } from 'react-redux';

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
import { LaoHooks } from '../hooks';
import { selectIsLaoOrganizer } from '../reducer';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

/**
 * AttendeeScreen: lists LAO properties and past/ongoing/future events.
 * By default, only the past and present section are open.
 */
const EventsScreen = () => {
  const EventList = LaoHooks.useEventListComponent();

  return (
    <ScreenWrapper>
      <EventList />
    </ScreenWrapper>
  );
};

export default EventsScreen;

const styles = StyleSheet.create({
  buttons: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  },
  button: {
    marginLeft: Spacing.x1,
  },
});

/**
 * Component rendered in the top middle of the navgiation bar when looking
 * at the lao home screen. Makes sure it shows the name of the lao and
 * not just some static string.
 */
export const EventsScreenHeader = () => {
  const lao = LaoHooks.useCurrentLao();
  return <Text style={Typography.topNavigationHeading}>{lao.name}</Text>;
};

/**
 * Component rendered in the top left of the navigation bar
 * Shows a disconnect icon for disconnecting from the lao
 */
export const EventsScreenHeaderLeft = () => {
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
export const EventsScreenHeaderRight = () => {
  const [modalVisible, setModalVisible] = useState(false);

  const lao = LaoHooks.useCurrentLao();
  const encodeLaoConnection = LaoHooks.useEncodeLaoConnectionForQRCode();
  const isOrganizer = useSelector(selectIsLaoOrganizer);
  const CreateEventButton = LaoHooks.useCreateEventButtonComponent();

  return (
    <>
      <View style={styles.buttons}>
        <PoPTouchableOpacity
          onPress={() => setModalVisible(!modalVisible)}
          containerStyle={styles.button}>
          <PoPIcon name="qrCode" color={Color.inactive} size={Icon.size} />
        </PoPTouchableOpacity>
        {isOrganizer && (
          <View style={styles.button}>
            <CreateEventButton />
          </View>
        )}

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
              {STRINGS.lao_properties_modal_heading}
            </ModalHeader>

            <LaoProperties />

            <Text style={[Typography.base, Typography.important]}>{STRINGS.lao_qr_code_title}</Text>
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
