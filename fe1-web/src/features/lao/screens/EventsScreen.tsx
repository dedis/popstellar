import React, { useState } from 'react';
import { Modal, StyleSheet, View } from 'react-native';
import { ScrollView, TouchableWithoutFeedback } from 'react-native-gesture-handler';
import { useSelector } from 'react-redux';

import { PoPIcon } from 'core/components';
import DrawerMenuButton from 'core/components/DrawerMenuButton';
import ModalHeader from 'core/components/ModalHeader';
import NavigationPadding from 'core/components/NavigationPadding';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { Color, Icon, ModalStyles } from 'core/styles';
import STRINGS from 'resources/strings';

import { LaoProperties } from '../components';
import { LaoHooks } from '../hooks';
import { selectIsLaoOrganizer } from '../reducer';

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
  headerLeftContainer: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
  },
  buttons: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  },
  button: {
    marginLeft: Icon.buttonMargin,
  },
  lastButton: {},
});

/**
 * Component rendered in the top left of the navigation bar
 * Shows a disconnect icon for disconnecting from the lao
 */
export const EventsScreenHeaderLeft = () => {
  const isOrganizer = LaoHooks.useIsLaoOrganizer();

  return (
    <View style={styles.headerLeftContainer}>
      <DrawerMenuButton />
      <NavigationPadding paddingAmount={isOrganizer ? 1 : 0} nextToIcon />
    </View>
  );
};

/**
 * Component rendered in the top right of the navigation bar
 * Shows a qr code icon for showing the lao connection qr code
 * and a bell icon for accessing the notifications menu
 */
export const EventsScreenHeaderRight = () => {
  const [modalVisible, setModalVisible] = useState(false);

  const isOrganizer = useSelector(selectIsLaoOrganizer);
  const CreateEventButton = LaoHooks.useCreateEventButtonComponent();

  return (
    <View style={styles.buttons}>
      <PoPTouchableOpacity
        onPress={() => setModalVisible(!modalVisible)}
        containerStyle={styles.lastButton}>
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
        </ScrollView>
      </Modal>
    </View>
  );
};
