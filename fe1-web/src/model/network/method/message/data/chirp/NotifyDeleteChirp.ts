import { Hash, Timestamp } from 'model/objects';
import { ProtocolError } from 'model/network/ProtocolError';
import { validateDataObject } from 'model/network/validation';
import { checkTimestampStaleness } from '../Checker';
import { ActionType, MessageData, ObjectType } from '../MessageData';

/** Data sent to broadcast a chirp to the general social channel */
export class NotifyDeleteChirp implements MessageData {
  public readonly object: ObjectType = ObjectType.CHIRP;

  public readonly action: ActionType = ActionType.NOTIFY_DELETE;

  // The ID of the chirp
  public readonly chirp_id: Hash;

  // The channel to which the chirp has been deleted
  public readonly channel: string;

  // The timestamp at which the chirp has been deleted
  public readonly timestamp: Timestamp;

  constructor(msg: Partial<NotifyDeleteChirp>) {
    if (!msg.chirp_id) {
      throw new ProtocolError("Undefined 'id' parameter encountered during 'NotifyDeleteChirp'");
    }
    this.chirp_id = msg.chirp_id;

    if (!msg.timestamp) {
      throw new ProtocolError(
        "Undefined 'timestamp' parameter encountered during 'NotifyDeleteChirp'",
      );
    }
    checkTimestampStaleness(msg.timestamp);
    this.timestamp = msg.timestamp;

    if (!msg.channel) {
      throw new ProtocolError(
        "Undefined 'channel' parameter encountered during 'NotifyDeleteChirp'",
      );
    }
    this.channel = msg.channel;
  }

  /**
   * Creates a NotifyDeleteChirp object from a given JSON object.
   *
   * @param obj - The given JSON object
   */
  public static fromJson(obj: any): NotifyDeleteChirp {
    const { errors } = validateDataObject(ObjectType.CHIRP, ActionType.NOTIFY_DELETE, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid receive chirp\n\n${errors}`);
    }

    return new NotifyDeleteChirp({
      ...obj,
      chirp_id: new Hash(obj.chirp_id),
      timestamp: new Timestamp(obj.timestamp),
    });
  }
}
