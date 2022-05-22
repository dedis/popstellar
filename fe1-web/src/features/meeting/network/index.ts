import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';

import { MeetingConfiguration } from '../interface';
import { handleMeetingCreateMessage, handleMeetingStateMessage } from './MeetingHandler';
import { CreateMeeting, StateMeeting } from './messages';

/**
 * Configures the network callbacks in a MessageRegistry.
 *
 * @param configuration - The configuration object for the meeting feature
 */
export const configureNetwork = (configuration: MeetingConfiguration) => {
  configuration.messageRegistry.add(
    ObjectType.MEETING,
    ActionType.CREATE,
    handleMeetingCreateMessage(configuration.addEvent),
    CreateMeeting.fromJson,
  );

  configuration.messageRegistry.add(
    ObjectType.MEETING,
    ActionType.STATE,
    handleMeetingStateMessage(
      configuration.getLaoById,
      configuration.getEventById,
      configuration.updateEvent,
    ),
    StateMeeting.fromJson,
  );
};
