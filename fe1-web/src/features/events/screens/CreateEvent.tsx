import { CompositeScreenProps } from '@react-navigation/core';
import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import React from 'react';
import { Text, View } from 'react-native';

import { TextBlock, Button } from 'core/components';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoOrganizerParamList } from 'core/navigation/typing/LaoOrganizerParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Typography } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';

import { EventHooks } from '../hooks';

/**
 * Navigation panels to help manoeuvre through events creation.
 */

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoOrganizerParamList, typeof STRINGS.navigation_lao_organizer_home>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;
const CreateEvent = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const eventTypes = EventHooks.useEventTypes();

  return (
    <View style={containerStyles.flex}>
      <TextBlock text={STRINGS.create_description} />

      {eventTypes.map((eventType) => (
        <Button
          key={eventType.eventType}
          onPress={() =>
            navigation.navigate(STRINGS.navigation_app_lao, {
              screen: STRINGS.navigation_lao_events,
              params: {
                screen: eventType.navigationNames.createEvent,
              },
            })
          }>
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
