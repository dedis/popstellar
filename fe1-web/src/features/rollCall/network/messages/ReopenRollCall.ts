import { validateDataObject } from 'model/network/validation';
import { Hash, Timestamp, ProtocolError } from 'model/objects';
import { ActionType, ObjectType } from 'model/network/method/message/data/MessageData';

import { OpenRollCall } from './OpenRollCall';

/** Data sent to reopen a Roll-Call event */
export class ReopenRollCall extends OpenRollCall {
  public readonly object: ObjectType = ObjectType.ROLL_CALL;

  public readonly action: ActionType = ActionType.REOPEN;

  /**
   * Creates a ReopenRollCall object from a given object.
   *
   * @param obj
   */
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
