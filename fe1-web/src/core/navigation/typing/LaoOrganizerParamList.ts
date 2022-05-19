import { ViewStyle } from 'react-native';

import STRINGS from 'resources/strings';

interface CreateEventParamsType {
  view: ViewStyle;
  viewVertical: ViewStyle;
}

export type LaoOrganizerParamList = {
  [STRINGS.navigation_lao_organizer_home]: undefined;
  [STRINGS.navigation_lao_organizer_create_event]: undefined;
  [STRINGS.navigation_lao_organizer_creation_meeting]: CreateEventParamsType;
  [STRINGS.navigation_lao_organizer_creation_roll_call]: CreateEventParamsType;
  [STRINGS.navigation_lao_organizer_creation_election]: CreateEventParamsType;
  [STRINGS.navigation_lao_organizer_open_roll_call]: { rollCallId: string };
};
