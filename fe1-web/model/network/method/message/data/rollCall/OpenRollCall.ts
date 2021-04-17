import { Hash, Timestamp } from 'model/objects';
import { ProtocolError } from 'model/network/ProtocolError';
import { validateDataObject } from 'model/network/validation';
import { ActionType, MessageData, ObjectType } from '../MessageData';
import { checkTimestampStaleness } from '../Checker';

export class OpenRollCall implements MessageData {
  public readonly object: ObjectType = ObjectType.ROLL_CALL;

  public readonly action: ActionType = ActionType.OPEN;

  public readonly updateId: Hash;

  public readonly opens: Hash;

  public readonly openedAt: Timestamp;

  constructor(msg: Partial<OpenRollCall>) {
    if (!msg.openedAt) {
      throw new ProtocolError("Undefined 'openedAt' parameter encountered during 'OpenRollCall'");
    }
    checkTimestampStaleness(msg.openedAt);
    this.openedAt = msg.openedAt;

    if (!msg.updateId) {
      throw new ProtocolError("Undefined 'updateId' parameter encountered during 'OpenRollCall'");
    }
    this.updateId = msg.updateId;

    if (!msg.opens) {
      throw new ProtocolError("Undefined 'opens' parameter encountered during 'OpenRollCall'");
    }
    this.opens = msg.opens;
  }

  public static fromJson(obj: any): OpenRollCall {
    const { errors } = validateDataObject(ObjectType.ROLL_CALL, ActionType.OPEN, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid open roll call\n\n${errors}`);
    }

    return new OpenRollCall({
      ...obj,
      openedAt: new Timestamp(obj.opened_at),
      updateId: new Hash(obj.update_id),
      opens: new Hash(obj.opens),
    });
  }
}
