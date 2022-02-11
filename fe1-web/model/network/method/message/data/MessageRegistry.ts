import {
  ActionType, MessageData, ObjectType, SignatureType,
} from './MessageData';
import { ExtendedMessage } from '../ExtendedMessage';
import { CreateLao, StateLao, UpdateLao } from './lao';
import { CreateMeeting, StateMeeting } from './meeting';
import {
  CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall,
} from './rollCall';
import {
  CastVote, ElectionResult, EndElection, SetupElection,
} from './election';
import { WitnessMessage } from './witness';
import {
  AddChirp, DeleteChirp, NotifyAddChirp, NotifyDeleteChirp,
} from './chirp';
import { AddReaction } from './reaction';

type HandleFunction = (msg: ExtendedMessage) => boolean;

/**
 * Represents an entry of the MessageRegistry.
 */
interface MessageEntry {
  // Function to handle this type of message
  handle?: HandleFunction;

  // Function to build this type of message from Json
  build?: Function;

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
    [k(ObjectType.LAO, ActionType.CREATE),
      {
        build: CreateLao.fromJson,
        signature: SignatureType.KEYPAIR,
      }],
    [k(ObjectType.LAO, ActionType.STATE),
      {
        build: StateLao.fromJson,
        signature: SignatureType.KEYPAIR,
      }],
    [k(ObjectType.LAO, ActionType.UPDATE_PROPERTIES),
      {
        build: UpdateLao.fromJson,
        signature: SignatureType.KEYPAIR,
      }],

    // Meeting
    [k(ObjectType.MEETING, ActionType.CREATE),
      {
        build: CreateMeeting.fromJson,
        signature: SignatureType.KEYPAIR,
      }],
    [k(ObjectType.MEETING, ActionType.STATE),
      {
        build: StateMeeting.fromJson,
        signature: SignatureType.KEYPAIR,
      }],

    // Roll call
    [k(ObjectType.ROLL_CALL, ActionType.CREATE),
      {
        build: CreateRollCall.fromJson,
        signature: SignatureType.KEYPAIR,
      }],
    [k(ObjectType.ROLL_CALL, ActionType.OPEN),
      {
        build: OpenRollCall.fromJson,
        signature: SignatureType.KEYPAIR,
      }],
    [k(ObjectType.ROLL_CALL, ActionType.CLOSE),
      {
        build: CloseRollCall.fromJson,
        signature: SignatureType.KEYPAIR,
      }],
    [k(ObjectType.ROLL_CALL, ActionType.REOPEN),
      {
        build: ReopenRollCall.fromJson,
        signature: SignatureType.KEYPAIR,
      }],

    // Election
    [k(ObjectType.ELECTION, ActionType.SETUP),
      {
        build: SetupElection.fromJson,
        signature: SignatureType.KEYPAIR,
      }],
    [k(ObjectType.ELECTION, ActionType.CAST_VOTE),
      {
        build: CastVote.fromJson,
        signature: SignatureType.POP_TOKEN,
      }],
    [k(ObjectType.ELECTION, ActionType.END),
      {
        build: EndElection.fromJson,
        signature: SignatureType.KEYPAIR,
      }],
    [k(ObjectType.ELECTION, ActionType.RESULT),
      {
        build: ElectionResult.fromJson,
        signature: SignatureType.KEYPAIR,
      }],

    // Witness
    [k(ObjectType.MESSAGE, ActionType.WITNESS),
      {
        build: WitnessMessage.fromJson,
        signature: SignatureType.KEYPAIR,
      }],

    // Chirps
    [k(ObjectType.CHIRP, ActionType.ADD),
      {
        build: AddChirp.fromJson,
        signature: SignatureType.POP_TOKEN,
      }],
    [k(ObjectType.CHIRP, ActionType.NOTIFY_ADD),
      {
        build: NotifyAddChirp.fromJson,
        signature: SignatureType.KEYPAIR,
      }],
    [k(ObjectType.CHIRP, ActionType.DELETE),
      {
        build: DeleteChirp.fromJson,
        signature: SignatureType.POP_TOKEN,
      }],
    [k(ObjectType.CHIRP, ActionType.NOTIFY_DELETE),
      {
        build: NotifyDeleteChirp.fromJson,
        signature: SignatureType.KEYPAIR,
      }],

    // Reactions
    [k(ObjectType.REACTION, ActionType.ADD),
      {
        build: AddReaction.fromJson,
        signature: SignatureType.POP_TOKEN,
      }],
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
   * Builds a message from a MessageData by calling the corresponding function.
   *
   * @param data -The type of message to be built
   * @returns MessageData - The built message
   */
  buildMessageData(data: MessageData): MessageData {
    const messageEntry = this.mapping.get(k(data.object, data.action));
    if (messageEntry === undefined) {
      throw new Error(`Message ${data.object} ${data.action} is not contained in MessageRegistry`);
    }
    const { build } = messageEntry;
    if (build === undefined) {
      throw new Error(`Message ${data.object} ${data.action} does not have an 'build' property in MessageRegistry`);
    }
    return build(data);
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
