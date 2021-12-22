import { Hash, Timestamp } from 'model/objects';
import { ProtocolError } from 'model/network/ProtocolError';
import { validateDataObject } from 'model/network/validation';
import { ActionType, MessageData, ObjectType } from '../MessageData';
import { checkTimestampStaleness } from '../Checker';

/** Data sent to broadcast a chirp to the general social channel */
export class AddChirpBroadcast implements MessageData {
  public readonly object: ObjectType = ObjectType.CHIRP;

  public readonly action: ActionType = ActionType.NOTIFY_ADD;

  // The ID of the chirp
  public readonly chirp_id: Hash;

  // The channel to which the chirp has been posted
  public readonly channel: string;

  // The timestamp at which the chirp has been posted
  public readonly timestamp: Timestamp;

  constructor(msg: Partial<AddChirpBroadcast>) {
    if (!msg.chirp_id) {
      throw new ProtocolError('Undefined \'id\' parameter encountered during \'AddChirpBroadcast\'');
    }
    this.chirp_id = msg.chirp_id;

    if (!msg.timestamp) {
      throw new ProtocolError('Undefined \'timestamp\' parameter encountered during \'AddChirpBroadcast\'');
    }
    checkTimestampStaleness(msg.timestamp);
    this.timestamp = msg.timestamp;

    if (!msg.channel) {
      throw new ProtocolError('Undefined \'channel\' parameter encountered during \'AddChirpBroadcast\'');
    }
    this.channel = msg.channel;
  }

  /**
   * Creates an AddChirpBroadcast object from a given JSON object.
   *
   * @param obj - The given JSON object
   */
  public static fromJson(obj: any): AddChirpBroadcast {
    const { errors } = validateDataObject(ObjectType.CHIRP, ActionType.NOTIFY_ADD, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid receive chirp\n\n${errors}`);
    }

    return new AddChirpBroadcast({
      ...obj,
      chirp_id: new Hash(obj.chirp_id),
      timestamp: new Timestamp(obj.timestamp),
    });
  }
}
