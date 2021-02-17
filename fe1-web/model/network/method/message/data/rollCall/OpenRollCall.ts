import { Hash, Timestamp, Lao } from 'model/objects';
import { OpenedLaoStore } from 'store';
import { ProtocolError } from 'model/network/ProtocolError';
import { ActionType, MessageData, ObjectType } from '../MessageData';
import { checkTimestampStaleness } from '../Checker';

export class OpenRollCall implements MessageData {
  public readonly object: ObjectType = ObjectType.ROLL_CALL;

  public readonly action: ActionType = ActionType.OPEN;

  public readonly id: Hash;

  public readonly start: Timestamp;

  constructor(msg: Partial<OpenRollCall>) {
    if (!msg.start) throw new ProtocolError('Undefined \'start\' parameter encountered during \'OpenRollCall\'');
    checkTimestampStaleness(msg.start);
    this.start = msg.start;

    if (!msg.id) throw new ProtocolError('Undefined \'id\' parameter encountered during \'CreateLao\'');

    // FIXME: implementation not finished, get event from storage,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const lao: Lao = OpenedLaoStore.get();
    /*
    const expectedHash = Hash.fromStringArray(
      eventTags.ROLL_CALL, lao.id.toString(), lao.creation.toString(), ROLLCALLNAME,
    );
    if (!expectedHash.equals(msg.id))
      throw new ProtocolError(
        'Invalid \'id\' parameter encountered during \'CreateLao\': unexpected id value'
      ); */
    this.id = msg.id;
  }

  public static fromJson(obj: any): OpenRollCall {
    // FIXME add JsonSchema validation to all "fromJson"
    const correctness = true;

    return correctness
      ? new OpenRollCall({
        ...obj,
        start: new Timestamp(obj.start),
        id: new Hash(obj.id),
      })
      : (() => { throw new ProtocolError('add JsonSchema error message'); })();
  }
}
