import { useNavigation } from '@react-navigation/native';
import React from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';

import { TextBlock, Button } from 'core/components';
import { Typography, Views } from 'core/styles';
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
        <Button
          key={`wide-btn-view-${eventType.eventType}`}
          onPress={() => navigation.navigate(eventType.navigationNames.createEvent, styleEvents)}>
          <Text style={[Typography.base, Typography.centered, Typography.negative]}>
            {eventType.eventType}
          </Text>
        </Button>
      ))}

      <Button onPress={navigation.goBack}>
        <Text style={[Typography.base, Typography.centered, Typography.negative]}>
          {STRINGS.general_button_cancel}
        </Text>
      </Button>
    </View>
  );
};

export default CreateEvent;
