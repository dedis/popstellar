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
 * contains all types of MessageData as keys and their corresponding signature type.
 */
export class MessageRegistry {
  private readonly mapping = new Map<string, MessageEntry>([
    // Lao
    [k(ObjectType.LAO, ActionType.CREATE), { signature: SignatureType.KEYPAIR }],
    [k(ObjectType.LAO, ActionType.STATE), { signature: SignatureType.KEYPAIR }],
    [k(ObjectType.LAO, ActionType.UPDATE_PROPERTIES), { signature: SignatureType.KEYPAIR }],

    // Meeting
    [k(ObjectType.MEETING, ActionType.CREATE), { signature: SignatureType.KEYPAIR }],
    [k(ObjectType.MEETING, ActionType.STATE), { signature: SignatureType.KEYPAIR }],

    // Roll call
    [k(ObjectType.ROLL_CALL, ActionType.CREATE), { signature: SignatureType.KEYPAIR }],
    [k(ObjectType.ROLL_CALL, ActionType.OPEN), { signature: SignatureType.KEYPAIR }],
    [k(ObjectType.ROLL_CALL, ActionType.CLOSE), { signature: SignatureType.KEYPAIR }],
    [k(ObjectType.ROLL_CALL, ActionType.REOPEN), { signature: SignatureType.KEYPAIR }],

    // Election
    [k(ObjectType.ELECTION, ActionType.SETUP), { signature: SignatureType.KEYPAIR }],
    [k(ObjectType.ELECTION, ActionType.CAST_VOTE), { signature: SignatureType.POP_TOKEN }],
    [k(ObjectType.ELECTION, ActionType.END), { signature: SignatureType.KEYPAIR }],
    [k(ObjectType.ELECTION, ActionType.RESULT), { signature: SignatureType.KEYPAIR }],

    // Witness
    [k(ObjectType.MESSAGE, ActionType.WITNESS), { signature: SignatureType.KEYPAIR }],

    // Chirps
    [k(ObjectType.CHIRP, ActionType.ADD), { signature: SignatureType.POP_TOKEN }],
    [k(ObjectType.CHIRP, ActionType.NOTIFY_ADD), { signature: SignatureType.KEYPAIR }],
    [k(ObjectType.CHIRP, ActionType.DELETE), { signature: SignatureType.POP_TOKEN }],
    [k(ObjectType.CHIRP, ActionType.NOTIFY_DELETE), { signature: SignatureType.KEYPAIR }],

    // Reactions
    [k(ObjectType.REACTION, ActionType.ADD), { signature: SignatureType.POP_TOKEN }],
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

  /**
   * Handles a message by calling the corresponding function.
   *
   * @param msg - The message to be handled
   * @returns boolean - Telling if the message has been processed or not
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

  /**
   * Gets the signature type of a message data.
   *
   * @param data - The message data we want to know how to sign
   * @returns SignatureType - The type of signature of the message
   */
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
