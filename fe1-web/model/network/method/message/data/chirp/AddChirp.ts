import { Hash, Timestamp } from 'model/objects';
import { ProtocolError } from 'model/network/ProtocolError';
import { validateDataObject } from 'model/network/validation';
import { ActionType, MessageData, ObjectType } from '../MessageData';
import { checkTimestampStaleness } from '../Checker';

export class AddChirp implements MessageData {
  public readonly object: ObjectType = ObjectType.CHIRP;

  public readonly action: ActionType = ActionType.ADD;

  public readonly text: string;

  public readonly parent_id?: Hash;

  public readonly timestamp: Timestamp;

  constructor(msg: Partial<AddChirp>) {
    if (!msg.text) {
      throw new ProtocolError('Undefined \'text\' parameter encountered during \'AddChirp\'');
    }
    if (msg.text.length > 280) {
      throw new ProtocolError('Max 280 characters');
    }
    this.text = msg.text;

    if (!msg.timestamp) {
      throw new ProtocolError('Undifined \'timestamp\' parameter encountered during \'AddChirp\'');
    }
    checkTimestampStaleness(msg.timestamp);
    this.timestamp = msg.timestamp;

    if (msg.parent_id) {
      this.parent_id = msg.parent_id;
    }
  }

  public static fromJson(obj: any): AddChirp {
    const { errors } = validateDataObject(ObjectType.CHIRP, ActionType.ADD, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid chirp add\n\n${errors}`);
    }

    return new AddChirp({
      ...obj,
      timestamp: new Timestamp(obj.timestamp),
    });
  }
}
