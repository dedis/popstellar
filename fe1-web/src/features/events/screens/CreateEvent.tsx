import { useNavigation } from '@react-navigation/native';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { TextBlock, WideButtonView } from 'core/components';
import { Timestamp } from 'core/objects';
import { Views } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';

import { LaoEventType } from '../objects';

/**
 * Navigation panels to help manoeuvre through events creation.
 */

const FIVE_MINUTES_IN_SECONDS = 300;

const styleEvents = StyleSheet.create({
  view: {
    ...Views.base,
    flexDirection: 'row',
    zIndex: 3,
  } as ViewStyle,
  viewVertical: {
    ...Views.base,
    flexDirection: 'column',
    zIndex: 3,
  } as ViewStyle,
});

const CreateEvent = () => {
  // FIXME: Navigation should use a defined type here (instead of any)
  const navigation = useNavigation<any>();

  const navigateToPanel = (type: string) => {
    switch (type) {
      case LaoEventType.MEETING:
        navigation.navigate(STRINGS.organizer_navigation_creation_meeting, styleEvents);
        break;

      case LaoEventType.ROLL_CALL:
        navigation.navigate(STRINGS.organizer_navigation_creation_roll_call, styleEvents);
        break;

      case LaoEventType.ELECTION:
        navigation.navigate(STRINGS.organizer_navigation_creation_election, styleEvents);
        break;

      default:
        console.debug(`${type} (default event => no mapping in CreateEvent.tsx)`);
    }
  };

  return (
    <View style={containerStyles.flex}>
      <TextBlock text={STRINGS.create_description} />

      {Object.values(LaoEventType).map((type: string) => (
        <WideButtonView
          title={type}
          key={`wide-btn-view-${type}`}
          onPress={() => navigateToPanel(type)}
        />
      ))}

      <WideButtonView title={STRINGS.general_button_cancel} onPress={navigation.goBack} />
    </View>
  );
};

/**
 * Function called when the user confirms an event creation. If the end is in the past, it will tell
 * the user and cancel the creation. If the event starts more than 5 minutes in the past, it will
 * ask if it can start now. Otherwise, the event will simply be created.
 *
 * @param start - The start time of the event
 * @param end - The end time of the event
 * @param createEvent - The function which creates the event
 * @param setStartModalIsVisible - The function which sets the visibility of the modal on starting
 * time being in past
 * @param setEndModalIsVisible - The function which sets the visibility of the modal on ending time
 * being in past
 */
export const onConfirmPress = (
  start: Timestamp,
  end: Timestamp,
  createEvent: Function,
  setStartModalIsVisible: Function,
  setEndModalIsVisible: Function,
) => {
  const now = Timestamp.EpochNow();

  if (end.before(now)) {
    setEndModalIsVisible(true);
  } else if (now.after(start.addSeconds(FIVE_MINUTES_IN_SECONDS))) {
    setStartModalIsVisible(true);
  } else {
    createEvent();
  }
};

export default CreateEvent;
