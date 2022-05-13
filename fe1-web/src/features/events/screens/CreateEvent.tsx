import { useNavigation } from '@react-navigation/native';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { TextBlock, WideButtonView } from 'core/components';
import { Views } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';

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

export default CreateEvent;
