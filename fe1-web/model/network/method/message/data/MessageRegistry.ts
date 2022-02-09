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
  handle?: HandleFunction;

  // Informs how the message should be signed
  signature?: SignatureType;
}

// Generates a string key of the form "object, action" for the MessageRegistry mapping
const k = (object: ObjectType, action: ActionType): string => `${object}, ${action}`;

/**
 * Registry of message data classes and their corresponding properties. By default, it already
 * contains all types of MessageData as keys.
 */
export class MessageRegistry {
  private readonly mapping = new Map<string, MessageEntry>([
    // Lao
    [k(ObjectType.LAO, ActionType.CREATE), {}],
    [k(ObjectType.LAO, ActionType.STATE), {}],
    [k(ObjectType.LAO, ActionType.UPDATE_PROPERTIES), {}],

    // Meeting
    [k(ObjectType.MEETING, ActionType.CREATE), {}],
    [k(ObjectType.MEETING, ActionType.STATE), {}],

    // Roll call
    [k(ObjectType.ROLL_CALL, ActionType.CREATE), {}],
    [k(ObjectType.ROLL_CALL, ActionType.OPEN), {}],
    [k(ObjectType.ROLL_CALL, ActionType.CLOSE), {}],
    [k(ObjectType.ROLL_CALL, ActionType.REOPEN), {}],

    // Election
    [k(ObjectType.ELECTION, ActionType.SETUP), {}],
    [k(ObjectType.ELECTION, ActionType.CAST_VOTE), {}],
    [k(ObjectType.ELECTION, ActionType.END), {}],
    [k(ObjectType.ELECTION, ActionType.RESULT), {}],

    // Witness
    [k(ObjectType.MESSAGE, ActionType.WITNESS), {}],

    // Chirp
    [k(ObjectType.CHIRP, ActionType.ADD), {}],
    [k(ObjectType.CHIRP, ActionType.DELETE), {}],

    // Reactions
    [k(ObjectType.REACTION, ActionType.ADD), {}],
  ]);

  /**
   * Adds a handle function to a type of message in the registry.
   *
   * @param object - The object of the message
   * @param action - The action of the message
   * @param handleFunc - The function that handles this type of message
   */
  addHandler(object: ObjectType, action: ActionType, handleFunc: HandleFunction) {
    const entry = this.mapping.get(k(object, action));
    if (entry === undefined) {
      throw new Error(`Message ${object} ${action} has not been initialized in MessageRegistry`);
    }
    entry.handle = handleFunc;
  }

  addSignature(object: ObjectType, action: ActionType, signature: SignatureType) {
    this.mapping.set(k(object, action), { signature: signature });
  }

  /**
   * Handles a message by calling the corresponding function.
   *
   * @param msg - The message to be handled
   */
  handleMessage(msg: ExtendedMessage): boolean {
    const data = msg.messageData;
    const messageEntry = this.mapping.get(k(data.object, data.action));
    if (messageEntry === undefined) {
      console.warn(`Message ${data.object} ${data.action} is not contained in MessageRegistry`);
      return false;
    }
    const { handle } = messageEntry;
    if (handle === undefined) {
      console.warn(`Message ${data.object} ${data.action} does not have an 'handle' property in MessageRegistry`);
      return false;
    }
    return handle(msg);
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
