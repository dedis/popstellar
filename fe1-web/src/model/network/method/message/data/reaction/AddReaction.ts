import { Hash, Timestamp } from 'model/objects';
import { ProtocolError } from 'model/network/ProtocolError';
import { validateDataObject } from 'model/network/validation';
import { checkTimestampStaleness } from '../Checker';
import { ActionType, MessageData, ObjectType } from '../MessageData';

/** Data sent to add a reaction */
export class AddReaction implements MessageData {
  public readonly object: ObjectType = ObjectType.REACTION;

  public readonly action: ActionType = ActionType.ADD;

  // Emoji indicating the added reaction
  public readonly reaction_codepoint: string;

  // id of the chirp message
  public readonly chirp_id: Hash;

  // timestamp of this add reaction request
  public readonly timestamp: Timestamp;

  constructor(msg: Partial<AddReaction>) {
    if (!msg.reaction_codepoint) {
      throw new ProtocolError('Undefined \'reaction_codepoint\' parameter encountered during \'AddReaction\'');
    }
    this.reaction_codepoint = msg.reaction_codepoint;

    if (!msg.chirp_id) {
      throw new ProtocolError('Undefined \'chirp_id\' parameter encountered during \'AddReaction\'');
    }
    this.chirp_id = msg.chirp_id;

    if (!msg.timestamp) {
      throw new ProtocolError('Undefined \'timestamp\' parameter encountered during \'AddReaction\'');
    }
    checkTimestampStaleness(msg.timestamp);
    this.timestamp = msg.timestamp;
  }

  /**
   * Creates an AddReaction object from a given object
   * @param obj
   */
  public static fromJson(obj: any): AddReaction {
    const { errors } = validateDataObject(ObjectType.REACTION, ActionType.ADD, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid reaction add\n\n${errors}`);
    }

    return new AddReaction({
      ...obj,
      timestamp: new Timestamp(obj.timestamp),
    });
  }
}
