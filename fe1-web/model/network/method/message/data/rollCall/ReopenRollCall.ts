import {
  EventTags, Hash, Lao, Timestamp,
} from 'model/objects';
import { OpenedLaoStore } from 'store';
import { ProtocolError } from 'model/network/ProtocolError';
import { validateDataObject } from 'model/network/validation';
import { ActionType, MessageData, ObjectType } from '../MessageData';
import { checkTimestampStaleness } from '../Checker';

export class ReopenRollCall implements MessageData {
  public readonly object: ObjectType = ObjectType.ROLL_CALL;

  public readonly action: ActionType = ActionType.REOPEN;

  public readonly update_id: Hash;

  public readonly opens: Hash;

  public readonly opened_at: Timestamp;

  constructor(msg: Partial<ReopenRollCall>) {
    if (!msg.opened_at) {
      throw new ProtocolError("Undefined 'opened_at' parameter encountered during 'ReopenRollCall'");
    }
    checkTimestampStaleness(msg.opened_at);
    this.opened_at = new Timestamp(msg.opened_at.toString());

    if (!msg.update_id) {
      throw new ProtocolError("Undefined 'update_id' parameter encountered during 'ReopenRollCall'");
    }

    if (!msg.opens) {
      throw new ProtocolError("Undefined 'opens' parameter encountered during 'ReopenRollCall'");
    }
    this.opens = msg.opens;

    if (!msg.update_id) {
      throw new ProtocolError("Undefined 'update_id' parameter encountered during 'ReopenRollCall'");
    }
    const lao: Lao = OpenedLaoStore.get();
    const expectedHash = Hash.fromStringArray(
      EventTags.ROLL_CALL, lao.id.toString(), this.opens.toString(), this.opened_at.toString(),
    );
    if (!expectedHash.equals(msg.update_id)) {
      throw new ProtocolError("Invalid 'update_id' parameter encountered during 'ReopenRollCall':"
        + ' re-computing the value yields a different result');
    }
    this.update_id = msg.update_id;
  }

  public static fromJson(obj: any): ReopenRollCall {
    const { errors } = validateDataObject(ObjectType.ROLL_CALL, ActionType.REOPEN, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid reopen roll call\n\n${errors}`);
    }

    return new ReopenRollCall({
      ...obj,
      opened_at: new Timestamp(obj.opened_at),
      update_id: new Hash(obj.update_id),
      opens: new Hash(obj.opens),
    });
  }
}
