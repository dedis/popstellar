import { useActionSheet } from '@expo/react-native-action-sheet';
import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React from 'react';

import { PoPIcon } from 'core/components';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Color, Icon } from 'core/styles';
import STRINGS from 'resources/strings';

import { EventHooks } from '../hooks';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

const CreateEventButton = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const types = EventHooks.useEventTypes();

  const { showActionSheetWithOptions } = useActionSheet();

  // release: hide meeting event
  const typesRestricted = types.filter((type) => type.eventName !== STRINGS.meeting_event_name);

  const onPress = () => {
    showActionSheetWithOptions(
      {
        options: typesRestricted
          // release: hide meeting event
          .map((type) => `${STRINGS.events_create_event} ${type.eventName}`)
          .concat([STRINGS.general_button_cancel]),
        cancelButtonIndex: typesRestricted.length,
      },
      (idx) => {
        if (idx !== undefined && idx < typesRestricted.length) {
          navigation.navigate(STRINGS.navigation_app_lao, {
            screen: STRINGS.navigation_lao_events,
            params: {
              screen: typesRestricted[idx].navigationNames.createEvent,
            },
          });
        } else {
          // cancel
        }
      },
    );
  };

  return (
    <PoPTouchableOpacity onPress={onPress} testID="create_event_selector">
      <PoPIcon name="create" color={Color.inactive} size={Icon.size} />
    </PoPTouchableOpacity>
  );
};

export default CreateEventButton;
