import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { checkTimestampStaleness } from 'core/network/validation/Checker';
import { Hash, ProtocolError, Timestamp } from 'core/objects';

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
      throw new ProtocolError(
        "Undefined 'reaction_codepoint' parameter encountered during 'AddReaction'",
      );
    }
    this.reaction_codepoint = msg.reaction_codepoint;

    if (!msg.chirp_id) {
      throw new ProtocolError("Undefined 'chirp_id' parameter encountered during 'AddReaction'");
    }
    this.chirp_id = msg.chirp_id;

    if (!msg.timestamp) {
      throw new ProtocolError("Undefined 'timestamp' parameter encountered during 'AddReaction'");
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
      reaction_codepoint: obj.reaction_codepoint,
      chirp_id: new Hash(obj.chirp_id),
      timestamp: new Timestamp(obj.timestamp),
    });
  }
}
