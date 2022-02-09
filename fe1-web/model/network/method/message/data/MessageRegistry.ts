import {
  ActionType, MessageData, ObjectType, SignatureType,
} from './MessageData';
import { ExtendedMessage } from '../ExtendedMessage';

type HandleFunction = (msg: ExtendedMessage) => boolean;

/**
 * Represents an entry of the MessageRegistry.
 */
interface MessageEntry {
  // Function to handle this type of message
  ingestion?: HandleFunction;

  // Informs how the message should be signed
  signature?: SignatureType;
}

// Generates a string key of the form "object, action" for the MessageRegistry mapping
const k = (object: ObjectType, action: ActionType): string => `${object}, ${action}`;

/**
 * Registry of message data classes and their corresponding properties.
 */
export class MessageRegistry {
  private readonly mapping: Map<string, MessageEntry> = new Map<string, MessageEntry>();

  addHandler(object: ObjectType, action: ActionType, handleFunc: HandleFunction) {
    this.mapping.set(k(object, action), { ingestion: handleFunc });
  }

  addSignature(object: ObjectType, action: ActionType, signature: SignatureType) {
    this.mapping.set(k(object, action), { signature: signature });
  }

  handleMessage(msg: ExtendedMessage): boolean {
    const data = msg.messageData;
    const messageEntry = this.mapping.get(k(data.object, data.action));
    if (messageEntry === undefined) {
      console.warn(`Message ${data.object} ${data.action} is not contained in MessageRegistry`);
      return false;
    }
    const { ingestion } = messageEntry;
    if (ingestion === undefined) {
      console.warn(`Message ${data.object} ${data.action} does not have an 'ingestion' property in MessageRegistry`);
      return false;
    }
    return ingestion(msg);
  }

  getSignatureType(data: MessageData): SignatureType {
    const messageEntry = this.mapping.get(k(data.object, data.action));
    if (messageEntry === undefined) {
      throw new Error(`Message ${data.object} ${data.action} is not contained in MessageRegistry`);
    }
    const { signature } = messageEntry;
    if (signature === undefined) {
      throw new Error(`Message ${data.object} ${data.action} does not have a 'signature' property in MessageRegistry`);
    }
    return signature;
  }
}
