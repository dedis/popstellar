import { MessageRegistry, ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { handleMeetingCreateMessage, handleMeetingStateMessage } from './MeetingHandler';
import { CreateMeeting, StateMeeting } from './messages';

/**
 * Configures the network callbacks in a MessageRegistry.
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configureNetwork(registry: MessageRegistry) {
  registry.add(
    ObjectType.MEETING,
    ActionType.CREATE,
    handleMeetingCreateMessage,
    CreateMeeting.fromJson,
  );
  registry.add(
    ObjectType.MEETING,
    ActionType.STATE,
    handleMeetingStateMessage,
    StateMeeting.fromJson,
  );
}
