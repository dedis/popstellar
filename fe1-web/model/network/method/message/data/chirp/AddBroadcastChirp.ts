import { Hash, Timestamp } from 'model/objects';
import { ProtocolError } from 'model/network/ProtocolError';
import { validateDataObject } from 'model/network/validation';
import { ActionType, MessageData, ObjectType } from '../MessageData';
import { checkTimestampStaleness } from '../Checker';

/** Data sent to receive a chirp */
export class AddBroadcastChirp implements MessageData {
  public readonly object: ObjectType = ObjectType.CHIRP;

  public readonly action: ActionType = ActionType.ADD_BROADCAST;

  // The ID of the received chirp
  public readonly id: Hash;

  // The channel to which the chirp has been posted
  public readonly channel: string;

  // The timestamp at which the chirp has been posted
  public readonly timestamp: Timestamp;

  constructor(msg: Partial<AddBroadcastChirp>) {
    if (!msg.id) {
      throw new ProtocolError('Undefined \'id\' parameter encountered during \'AddBroadcastChirp\'');
    }
    this.id = msg.id;

    if (!msg.timestamp) {
      throw new ProtocolError('Undefined \'timestamp\' parameter encountered during \'AddBroadcastChirp\'');
    }
    checkTimestampStaleness(msg.timestamp);
    this.timestamp = msg.timestamp;

    if (!msg.channel) {
      throw new ProtocolError('Undefined \'channel\' parameter encountered during \'AddBroadcastChirp\'');
    }
    this.channel = msg.channel;
  }

  /**
   * Creates a AddBroadcastChirp object from a given JSON object.
   *
   * @param obj - The given JSON object
   */
  public static fromJson(obj: any): AddBroadcastChirp {
    const { errors } = validateDataObject(ObjectType.CHIRP, ActionType.ADD_BROADCAST, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid receive chirp\n\n${errors}`);
    }

    return new AddBroadcastChirp({
      ...obj,
      id: new Hash(obj.id),
      timestamp: new Timestamp(obj.timestamp),
    });
  }
}
