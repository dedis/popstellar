import STRINGS from 'resources/strings';

export type LaoEventsParamList = {
  [STRINGS.navigation_lao_events_home]: undefined;
  [STRINGS.navigation_lao_events_create_event]: undefined;
  [STRINGS.navigation_lao_events_creation_meeting]: undefined;
  [STRINGS.navigation_lao_events_creation_roll_call]: undefined;
  [STRINGS.navigation_lao_events_creation_election]: undefined;
  [STRINGS.navigation_lao_events_open_roll_call]: { rollCallId: string };
};
