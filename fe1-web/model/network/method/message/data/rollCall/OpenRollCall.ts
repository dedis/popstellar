import {
  EventTags, Hash, Lao, Timestamp,
} from 'model/objects';
import { ProtocolError } from 'model/network/ProtocolError';
import { validateDataObject } from 'model/network/validation';
import { OpenedLaoStore } from 'store';
import { ActionType, MessageData, ObjectType } from '../MessageData';
import { checkTimestampStaleness } from '../Checker';

const paramError = (o: OpenRollCall) => `parameter encountered during roll call ${o.action}`;

export class OpenRollCall implements MessageData {
  public readonly object: ObjectType = ObjectType.ROLL_CALL;

  public readonly action: ActionType = ActionType.OPEN;

  public readonly update_id: Hash;

  public readonly opens: Hash;

  public readonly opened_at: Timestamp;

  constructor(msg: Partial<OpenRollCall>) {
    if (!msg.opened_at) {
      throw new ProtocolError(`Undefined 'opened_at' ${paramError(this)}`);
    }
    checkTimestampStaleness(msg.opened_at);
    this.opened_at = msg.opened_at;

    if (!msg.opens) {
      throw new ProtocolError(`Undefined 'opens' ${paramError(this)}`);
    }
    this.opens = msg.opens;

    if (!msg.update_id) {
      throw new ProtocolError(`Undefined 'update_id' ${paramError(this)}`);
    }
    const lao: Lao = OpenedLaoStore.get();
    const expectedHash = Hash.fromStringArray(
      EventTags.ROLL_CALL, lao.id.toString(), this.opens.toString(), this.opened_at.toString(),
    );
    if (!expectedHash.equals(msg.update_id)) {
      throw new ProtocolError(`Invalid 'update_id' ${paramError(this)}:`
        + ' re-computing the value yields a different result');
    }
    this.update_id = msg.update_id;
  }

  public static fromJson(obj: any): OpenRollCall {
    const { errors } = validateDataObject(ObjectType.ROLL_CALL, ActionType.OPEN, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid open roll call\n\n${errors}`);
    }

    return new OpenRollCall({
      ...obj,
      opened_at: new Timestamp(obj.opened_at),
      update_id: new Hash(obj.update_id),
      opens: new Hash(obj.opens),
    });
  }
}
