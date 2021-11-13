import { Hash, Timestamp } from 'model/objects';
import { ProtocolError } from 'model/network/ProtocolError';
import { validateDataObject } from 'model/network/validation';
import { ActionType, MessageData, ObjectType } from '../MessageData';
import { checkTimestampStaleness } from '../Checker';

/** Data sent to receive a chirp */
export class ReceiveChirp implements MessageData {
  public readonly object: ObjectType = ObjectType.CHIRP;

  public readonly action: ActionType = ActionType.RECEIVE;

  // The ID of the received chirp
  public readonly id: Hash;

  // The text of the chirp
  public readonly text: string;

  // The timestamp at which the chirp has been posted
  public readonly timestamp: Timestamp;

  // The parent ID of the chirp (if it is a reply)
  public readonly parentId?: Hash;

  constructor(msg: Partial<ReceiveChirp>) {
    if (!msg.id) {
      throw new ProtocolError('Undefined \'id\' parameter encountered during \'ReceiveChirp\'');
    }
    this.id = msg.id;

    if (!msg.text) {
      throw new ProtocolError('Undefined \'text\' parameter encountered during \'ReceiveChirp\'');
    }
    this.text = msg.text;

    if (!msg.timestamp) {
      throw new ProtocolError('Undefined \'id\' parameter encountered during \'ReceiveChirp\'');
    }
    checkTimestampStaleness(msg.timestamp);
    this.timestamp = msg.timestamp;

    if (msg.parentId) {
      this.parentId = msg.parentId;
    }
  }

  public static fromJson(obj: any): ReceiveChirp {
    const { errors } = validateDataObject(ObjectType.MEETING, ActionType.RECEIVE, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid receive chirp\n\n${errors}`);
    }

    return new ReceiveChirp({
      ...obj,
      id: new Hash(obj.id),
      timestamp: new Timestamp(obj.timestamp),
    });
  }
}
