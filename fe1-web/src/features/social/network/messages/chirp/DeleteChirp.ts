import { Hash, Timestamp } from 'model/objects';
import { ProtocolError } from 'model/network/ProtocolError';
import { validateDataObject } from 'model/network/validation';
import { ActionType, MessageData, ObjectType } from 'model/network/method/message/data/MessageData';
import { checkTimestampStaleness } from 'model/network/method/message/data/Checker';

/**
 * Data sent to remove a chirp
 */
export class DeleteChirp implements MessageData {
  public readonly object: ObjectType = ObjectType.CHIRP;

  public readonly action: ActionType = ActionType.DELETE;

  // The id of the chirp published
  public readonly chirp_id: Hash;

  // The timestamp of this deletion request
  public readonly timestamp: Timestamp;

  constructor(msg: Partial<DeleteChirp>) {
    if (!msg.chirp_id) {
      throw new ProtocolError("Undefined 'chirp_id' parameter encountered during 'RemoveChirp'");
    }
    this.chirp_id = msg.chirp_id;

    if (!msg.timestamp) {
      throw new ProtocolError("Undefined 'timestamp' parameter encountered during 'RemoveChirp'");
    }
    checkTimestampStaleness(msg.timestamp);
    this.timestamp = msg.timestamp;
  }

  /**
   * Creates a RemoveChirp object from a given object
   * @param obj
   */
  public static fromJson(obj: any): DeleteChirp {
    const { errors } = validateDataObject(ObjectType.CHIRP, ActionType.DELETE, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid chirp remove\n\n${errors}`);
    }

    return new DeleteChirp({
      ...obj,
      timestamp: new Timestamp(obj.timestamp),
    });
  }
}
