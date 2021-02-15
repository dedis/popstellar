import { Hash, Lao, Timestamp } from 'model/objects';
import { OpenedLaoStore } from 'store';
import { ProtocolError } from 'model/network/ProtocolError';
import { ActionType, MessageData, ObjectType } from '../MessageData';
import { checkTimestampStaleness } from '../Checker';

export class ReopenRollCall implements MessageData {
  public readonly object: ObjectType = ObjectType.ROLL_CALL;

  public readonly action: ActionType = ActionType.REOPEN;

  public readonly id: Hash;

  public readonly start: Timestamp;

  constructor(msg: Partial<ReopenRollCall>) {
    if (!msg.start) throw new ProtocolError('Undefined \'start\' parameter encountered during \'OpenRollCall\'');
    checkTimestampStaleness(msg.start);
    this.start = new Timestamp(msg.start.toString());

    if (!msg.id) throw new ProtocolError('Undefined \'id\' parameter encountered during \'CreateLao\'');
    const lao: Lao = OpenedLaoStore.get();
    /* // FIXME get event from storage
    const expectedHash = Hash.fromStringArray(
      eventTags.ROLL_CALL, lao.id.toString(), lao.creation.toString(), ROLLCALLNAME
    );
    if (!expectedHash.equals(msg.id))
      throw new ProtocolError(
        'Invalid \'id\' parameter encountered during \'CreateLao\': unexpected id value'
      );
    */
    this.id = new Hash(msg.id.toString());
  }

  public static fromJson(obj: any): ReopenRollCall {
    // FIXME add JsonSchema validation to all "fromJson"
    const correctness = true;

    return correctness
      ? new ReopenRollCall(obj)
      : (() => { throw new ProtocolError('add JsonSchema error message'); })();
  }
}
