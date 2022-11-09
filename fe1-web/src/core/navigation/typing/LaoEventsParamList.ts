import STRINGS from 'resources/strings';

interface ViewSingleParams {
  eventId: string;
  isOrganizer: boolean;

  // only used for single roll call view but static type checking breaks
  // if it is only added there :(
  attendeePopTokens?: string[];
}

export type LaoEventsParamList = {
  [STRINGS.navigation_lao_events_home]: undefined;
  [STRINGS.navigation_lao_events_create_event]: undefined;

  [STRINGS.navigation_lao_events_create_meeting]: undefined;
  [STRINGS.navigation_lao_events_view_single_meeting]: ViewSingleParams;

  [STRINGS.navigation_lao_events_create_roll_call]: undefined;
  [STRINGS.navigation_lao_events_view_single_roll_call]: ViewSingleParams;
  [STRINGS.navigation_lao_events_open_roll_call]: {
    rollCallId: string;
    attendeePopTokens: string[];
  };

  [STRINGS.navigation_lao_events_create_election]: undefined;
  [STRINGS.navigation_lao_events_view_single_election]: ViewSingleParams;
};
