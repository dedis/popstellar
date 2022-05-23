import { useActionSheet } from '@expo/react-native-action-sheet';
import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React from 'react';
import { StyleSheet, ViewStyle } from 'react-native';
import { TouchableOpacity } from 'react-native-gesture-handler';

import CreateIcon from 'core/components/icons/CreateIcon';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Color, Icon, ViewStyles } from 'core/styles';
import STRINGS from 'resources/strings';

import { EventHooks } from '../hooks';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

const styleEvents = StyleSheet.create({
  view: {
    ...ViewStyles.base,
    flexDirection: 'row',
    zIndex: 3,
  } as ViewStyle,
  viewVertical: {
    ...ViewStyles.base,
    flexDirection: 'column',
    zIndex: 3,
  } as ViewStyle,
});

const CreateEventButton = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const types = EventHooks.useEventTypes();

  const { showActionSheetWithOptions } = useActionSheet();

  const onPress = () => {
    showActionSheetWithOptions(
      {
        options: types
          .map((type) => `${STRINGS.events_create_event} ${type.eventName}`)
          .concat([STRINGS.general_button_cancel]),
        cancelButtonIndex: types.length,
      },
      (idx) => {
        if (idx !== undefined && idx < types.length) {
          navigation.push(STRINGS.navigation_app_lao, {
            screen: STRINGS.navigation_lao_events,
            params: {
              screen: types[idx].navigationNames.createEvent,
              params: styleEvents,
            },
          });
        } else {
          // cancel
        }
      },
    );
  };

  return (
    <TouchableOpacity onPress={onPress}>
      <CreateIcon color={Color.inactive} size={Icon.size} />
    </TouchableOpacity>
  );
};

export default CreateEventButton;
