import { Hash, Lao, Timestamp } from 'model/objects';
import { OpenedLaoStore } from 'store';
import { ProtocolError } from 'model/network/ProtocolError';
import { validateDataObject } from 'model/network/validation';
import { ActionType, MessageData, ObjectType } from '../MessageData';
import { checkTimestampStaleness } from '../Checker';

export class ReopenRollCall implements MessageData {
  public readonly object: ObjectType = ObjectType.ROLL_CALL;

  public readonly action: ActionType = ActionType.REOPEN;

  public readonly updateId: Hash;

  public readonly opens: Hash;

  public readonly openedAt: Timestamp;

  constructor(msg: Partial<ReopenRollCall>) {
    if (!msg.openedAt) {
      throw new ProtocolError("Undefined 'openedAt' parameter encountered during 'ReopenRollCall'");
    }
    checkTimestampStaleness(msg.openedAt);
    this.openedAt = new Timestamp(msg.openedAt.toString());

    if (!msg.updateId) {
      throw new ProtocolError("Undefined 'updateId' parameter encountered during 'ReopenRollCall'");
    }

    // FIXME: implementation not finished, get event from storage,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const lao: Lao = OpenedLaoStore.get();
    /*
    const expectedHash = Hash.fromStringArray(
      EventTags.ROLL_CALL, lao.id.toString(), lao.creation.toString(), ROLLCALLNAME
    );
    if (!expectedHash.equals(msg.id))
      throw new ProtocolError(
        'Invalid \'id\' parameter encountered during \'CreateLao\': unexpected id value'
      );
    */
    this.updateId = new Hash(msg.updateId.toString());

    if (!msg.opens) {
      throw new ProtocolError("Undefined 'opens' parameter encountered during 'OpenRollCall'");
    }
    this.opens = msg.opens;
  }

  public static fromJson(obj: any): ReopenRollCall {
    const { errors } = validateDataObject(ObjectType.ROLL_CALL, ActionType.REOPEN, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid reopen roll call\n\n${errors}`);
    }

    return new ReopenRollCall({
      ...obj,
      openedAt: new Timestamp(obj.opened_at),
      updateId: new Hash(obj.update_id),
      opens: new Hash(obj.opens),
    });
  }
}
