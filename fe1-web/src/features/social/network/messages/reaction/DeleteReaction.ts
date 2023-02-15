import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { Hash, ProtocolError, Timestamp } from 'core/objects';
import { MessageDataProperties } from 'core/types';

/** Data sent to add a reaction */
export class DeleteReaction implements MessageData {
  public readonly object: ObjectType = ObjectType.REACTION;

  public readonly action: ActionType = ActionType.DELETE;

  // message_id of the add reaction message
  public readonly reaction_id: Hash;

  // timestamp of this reaction deletion request
  public readonly timestamp: Timestamp;

  constructor(msg: MessageDataProperties<DeleteReaction>) {
    if (!msg.reaction_id) {
      throw new ProtocolError(
        "Undefined 'reaction_id' parameter encountered during 'DeleteReaction'",
      );
    }
    this.reaction_id = msg.reaction_id;

    if (!msg.timestamp) {
      throw new ProtocolError("Undefined 'timestamp' parameter encountered during 'AddReaction'");
    }
    this.timestamp = msg.timestamp;
  }

  /**
   * Creates an AddReaction object from a given object
   * @param obj
   */
  public static fromJson(obj: any): DeleteReaction {
    const { errors } = validateDataObject(ObjectType.REACTION, ActionType.DELETE, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid reaction add\n\n${errors}`);
    }

    return new DeleteReaction({
      reaction_id: new Hash(obj.reaction_id),
      timestamp: new Timestamp(obj.timestamp),
    });
  }
}
