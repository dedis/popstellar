import React from 'react';
import { StyleSheet, View } from 'react-native';
import { useSelector } from 'react-redux';

import ButtonPadding from 'core/components/ButtonPadding';
import DrawerMenuButton from 'core/components/DrawerMenuButton';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { Icon } from 'core/styles';

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
});

/**
 * Component rendered in the top left of the navigation bar
 * Shows a disconnect icon for disconnecting from the lao
 */
export const EventsScreenHeaderLeft = () => {
  return (
    <View style={styles.headerLeftContainer}>
      <DrawerMenuButton />
    </View>
  );
};

/**
 * Component rendered in the top right of the navigation bar
 * Shows a qr code icon for showing the lao connection qr code
 * and a bell icon for accessing the notifications menu
 */
export const EventsScreenHeaderRight = () => {
  const isOrganizer = useSelector(selectIsLaoOrganizer);
  const CreateEventButton = LaoHooks.useCreateEventButtonComponent();

  return (
    <View style={styles.buttons}>
      {isOrganizer ? (
        <View style={styles.button}>
          <CreateEventButton />
        </View>
      ) : (
        <ButtonPadding />
      )}
    </View>
  );
};
