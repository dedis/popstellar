import { Hash, ProtocolError } from 'core/objects';

import { ActionType, MessageData, ObjectType, SignatureType } from './MessageData';
import { ProcessableMessage } from './ProcessableMessage';

type HandleFunction = (msg: ProcessableMessage) => boolean;
type BuildFunction = (data: MessageData, laoId?: Hash) => MessageData;

const { LAO, MEETING, ROLL_CALL, ELECTION, MESSAGE, CHIRP, REACTION, COIN } = ObjectType;
const {
  CREATE,
  STATE,
  UPDATE_PROPERTIES,
  GREET,
  OPEN,
  CLOSE,
  REOPEN,
  KEY,
  SETUP,
  CAST_VOTE,
  END,
  RESULT,
  WITNESS,
  ADD,
  NOTIFY_ADD,
  DELETE,
  NOTIFY_DELETE,
  POST_TRANSACTION,
} = ActionType;
const { KEYPAIR, POP_TOKEN } = SignatureType;

/**
 * Represents an entry of the MessageRegistry.
 */
interface MessageEntry {
  // Function to handle this type of message
  handle?: HandleFunction;

  // Function to build this type of message from Json
  build?: BuildFunction;

  // Informs how the message should be signed
  signature?: SignatureType;
}

// Generates a string key of the form "object, action" for the MessageRegistry mapping
const k = (object: ObjectType, action: ActionType): string => `${object}, ${action}`;

/**
 * Registry of message data classes and their corresponding properties. By default, it already
 * contains all types of MessageData as keys and their corresponding signature type and build
 * function.
 */
export class MessageRegistry {
  private readonly mapping = new Map<string, MessageEntry>([
    // Lao
    [k(LAO, CREATE), { signature: KEYPAIR }],
    [k(LAO, STATE), { signature: KEYPAIR }],
    [k(LAO, UPDATE_PROPERTIES), { signature: KEYPAIR }],
    [k(LAO, GREET), { signature: KEYPAIR }],

    // Meeting
    [k(MEETING, CREATE), { signature: KEYPAIR }],
    [k(MEETING, STATE), { signature: KEYPAIR }],

    // Roll call
    [k(ROLL_CALL, CREATE), { signature: KEYPAIR }],
    [k(ROLL_CALL, OPEN), { signature: KEYPAIR }],
    [k(ROLL_CALL, CLOSE), { signature: KEYPAIR }],
    [k(ROLL_CALL, REOPEN), { signature: KEYPAIR }],

    // Election
    [k(ELECTION, KEY), { signature: KEYPAIR }],
    [k(ELECTION, SETUP), { signature: KEYPAIR }],
    [k(ELECTION, OPEN), { signature: KEYPAIR }],
    [k(ELECTION, CAST_VOTE), { signature: POP_TOKEN }],
    [k(ELECTION, END), { signature: KEYPAIR }],
    [k(ELECTION, RESULT), { signature: KEYPAIR }],

    // Witness
    [k(MESSAGE, WITNESS), { signature: KEYPAIR }],

    // Chirps
    [k(CHIRP, ADD), { signature: POP_TOKEN }],
    [k(CHIRP, NOTIFY_ADD), { signature: KEYPAIR }],
    [k(CHIRP, DELETE), { signature: POP_TOKEN }],
    [k(CHIRP, NOTIFY_DELETE), { signature: KEYPAIR }],

    // Reactions
    [k(REACTION, ADD), { signature: POP_TOKEN }],

    // Coin
    [k(COIN, POST_TRANSACTION), { signature: KEYPAIR }],
  ]);

  /**
   * Adds callback functions to manage a type of message in the registry.
   *
   * @param obj - The object of the message
   * @param action - The action of the message
   * @param handleFunc - The function that handles this type of message
   * @param buildFunc - The function to build this type of message
   */
  add(obj: ObjectType, action: ActionType, handleFunc: HandleFunction, buildFunc: BuildFunction) {
    const entry = this.getEntry({ object: obj, action: action });
    entry.handle = handleFunc;
    entry.build = buildFunc;
  }

  /**
   * Handles a message by calling the corresponding function.
   *
   * @param msg - The message to be handled
   * @returns boolean - Telling if the message has been processed or not
   */
  handleMessage(msg: ProcessableMessage): boolean {
    const data = msg.messageData;
    const messageEntry = this.getEntry(data);

    return messageEntry.handle!(msg);
  }

  /**
   * Builds a message from a MessageData by calling the corresponding function.
   *
   * @param data - The type of message to be built
   * @param laoId - The id of the lao this message was received from
   * @returns MessageData - The built message
   */
  buildMessageData(data: unknown, laoId?: Hash): MessageData {
    if (!MessageRegistry.isMessageData(data)) {
      throw new ProtocolError(`Data (${data}) is not a valid MessageData`);
    }
    const messageEntry = this.getEntry(data);
    return messageEntry.build!(data, laoId);
  }

  /**
   * Gets the signature to use for a MessageData.
   *
   * @param data - The type of message we want to know how to sign
   * @returns SignatureType - The type of signature of the message
   */
  getSignatureType(data: MessageData): SignatureType {
    const messageEntry = this.getEntry(data);
    return messageEntry.signature!;
  }

  /**
   * Verifies that all properties of the MessageEntries of the registry are defined.
   *
   * @throws Error if a property is undefined
   *
   * @remarks This must be executed before starting the application.
   */
  verifyEntries() {
    for (const [key, entry] of this.mapping) {
      if (entry.handle === undefined) {
        throw new Error(`Message '${key}' does not have a 'handle' property in MessageRegistry`);
      }
      if (entry.build === undefined) {
        throw new Error(`Message '${key}' does not have a 'build' property in MessageRegistry`);
      }
      if (entry.signature === undefined) {
        throw new Error(`Message '${key}' does not have a 'signature' property in MessageRegistry`);
      }
    }
  }

  /**
   * Gets an entry of the registry.
   *
   * @param data - The type of messages we want the entry
   * @throws Error if the messages is not in the registry
   *
   * @private
   */
  private getEntry(data: MessageData): MessageEntry {
    const key = k(data.object, data.action);
    const messageEntry = this.mapping.get(key);
    if (messageEntry === undefined) {
      throw new ProtocolError(`Message '${key}' is not contained in MessageRegistry`);
    }
    return messageEntry;
  }

  static isMessageData(value: unknown): value is MessageData {
    return typeof value === 'object' && value !== null && 'object' in value && 'action' in value;
  }
}
