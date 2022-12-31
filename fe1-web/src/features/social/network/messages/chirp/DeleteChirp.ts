import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { checkTimestampStaleness } from 'core/network/validation/Checker';
import { Hash, ProtocolError, Timestamp } from 'core/objects';

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
      chirp_id: new Hash(obj.chirp_id),
      timestamp: new Timestamp(obj.timestamp),
    });
  }
}
