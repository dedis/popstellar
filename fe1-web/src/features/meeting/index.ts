import { MeetingEventTypeComponent } from './components';
import { MeetingConfiguration, MeetingInterface, MEETING_FEATURE_IDENTIFIER } from './interface';
import { configureNetwork } from './network';
import * as screens from './screens';

/**
 * Configures the meeting feature
 *
 * @param configuration - The configuration object for the meeting feature
 */
export const configure = (configuration: MeetingConfiguration): MeetingInterface => {
  configureNetwork(configuration);

  return {
    identifier: MEETING_FEATURE_IDENTIFIER,
    eventTypeComponents: [MeetingEventTypeComponent],
    screens,
    context: {
      useCurrentLaoId: configuration.useCurrentLaoId,
    },
  };
};
