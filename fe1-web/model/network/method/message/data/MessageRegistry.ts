import { ActionType, ObjectType } from './MessageData';
import { ExtendedMessage } from '../ExtendedMessage';

type HandleFunction = (msg: ExtendedMessage) => boolean;

/**
 * Represents an entry of the MessageRegistry.
 */
interface MessageEntry {
  ingestion: HandleFunction; // Function to handle this type of message
}

// Generates a string key of the form "object, action" for the MessageRegistry mapping
const k = (object: ObjectType, action: ActionType): string => `${object}, ${action}`;

/**
 * Registry of message data classes and their corresponding properties.
 */
export class MessageRegistry {
  private mapping: Map<string, MessageEntry> = new Map<string, MessageEntry>();

  addHandler(object: ObjectType, action: ActionType, handleFunc: HandleFunction) {
    this.mapping.set(k(object, action), { ingestion: handleFunc });
  }

  handleMessage(msg: ExtendedMessage): boolean {
    const data = msg.messageData;
    const messageEntry = this.mapping.get(k(data.object, data.action));
    if (messageEntry === undefined) {
      console.warn(`Message ${data.object} ${data.action} is not contained in MessageRegistry`);
      return false;
    }
    return messageEntry.ingestion(msg);
  }
}
