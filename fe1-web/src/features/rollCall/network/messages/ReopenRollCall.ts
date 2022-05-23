import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { Hash, ProtocolError, Timestamp } from 'core/objects';

import { OpenRollCall } from './OpenRollCall';

/** Data sent to reopen a Roll-Call event */
export class ReopenRollCall extends OpenRollCall {
  public readonly object: ObjectType = ObjectType.ROLL_CALL;

  public readonly action: ActionType = ActionType.REOPEN;

  /**
   * Creates a ReopenRollCall object from a given object.
   *
   * @param obj The parsed json data
   * @param laoId The id of the lao this roll call belongs to
   */
  public static fromJson(obj: any, laoId?: Hash): ReopenRollCall {
    if (!laoId) {
      throw new Error(
        "Tried build a 'ReopenRollCall' message without knowing the associated lao id",
      );
    }

    const { errors } = validateDataObject(ObjectType.ROLL_CALL, ActionType.REOPEN, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid reopen roll call\n\n${errors}`);
    }

    return new ReopenRollCall(
      {
        ...obj,
        opened_at: new Timestamp(obj.opened_at),
        update_id: new Hash(obj.update_id),
        opens: new Hash(obj.opens),
      },
      laoId,
    );
  }
}
