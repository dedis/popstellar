import STRINGS from 'resources/strings';

export type LaoOrganizerParamList = {
  [STRINGS.navigation_lao_organizer_home]: undefined;
  [STRINGS.navigation_lao_organizer_create_event]: undefined;
  [STRINGS.navigation_lao_organizer_creation_meeting]: undefined;
  [STRINGS.navigation_lao_organizer_creation_roll_call]: undefined;
  [STRINGS.navigation_lao_organizer_creation_election]: undefined;
  [STRINGS.navigation_lao_organizer_open_roll_call]: { rollCallId: string };
};
