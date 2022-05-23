import { ViewStyle } from 'react-native';

import STRINGS from 'resources/strings';

interface CreateEventParamsType {
  view: ViewStyle;
  viewVertical: ViewStyle;
}

export type LaoEventsParamList = {
  [STRINGS.navigation_lao_events_home]: undefined;
  [STRINGS.navigation_lao_events_create_event]: undefined;
  [STRINGS.navigation_lao_events_creation_meeting]: CreateEventParamsType;
  [STRINGS.navigation_lao_events_creation_roll_call]: CreateEventParamsType;
  [STRINGS.navigation_lao_events_creation_election]: CreateEventParamsType;
  [STRINGS.navigation_lao_events_open_roll_call]: { rollCallId: string };
};
