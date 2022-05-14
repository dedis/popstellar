import { useNavigation } from '@react-navigation/native';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { TextBlock, WideButtonView } from 'core/components';
import { Views } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';

import { EventHooks } from '../hooks';

/**
 * Navigation panels to help manoeuvre through events creation.
 */

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
  const eventTypes = EventHooks.useEventTypes();

  return (
    <View style={containerStyles.flex}>
      <TextBlock text={STRINGS.create_description} />

      {eventTypes.map((eventType) => (
        <WideButtonView
          title={eventType.eventType}
          key={`wide-btn-view-${eventType.eventType}`}
          onPress={() => navigation.navigate(eventType.navigationNames.createEvent, styleEvents)}
        />
      ))}

      <WideButtonView title={STRINGS.general_button_cancel} onPress={navigation.goBack} />
    </View>
  );
};

export default CreateEvent;
