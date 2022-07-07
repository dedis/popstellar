import { MeetingEventType } from './components';
import { MeetingConfiguration, MeetingInterface, MEETING_FEATURE_IDENTIFIER } from './interface';
import { configureNetwork } from './network';
import { meetingReducer } from './reducer';
import { CreateMeetingScreen, ViewSingleMeetingScreen } from './screens';

/**
 * Configures the meeting feature
 *
 * @param configuration - The configuration object for the meeting feature
 */
export const configure = (configuration: MeetingConfiguration): MeetingInterface => {
  configureNetwork(configuration);

  return {
    identifier: MEETING_FEATURE_IDENTIFIER,
    eventTypes: [MeetingEventType],
    laoEventScreens: [CreateMeetingScreen, ViewSingleMeetingScreen],
    context: {
      useAssertCurrentLaoId: configuration.useAssertCurrentLaoId,
    },
    reducers: {
      ...meetingReducer,
    },
  };
};
