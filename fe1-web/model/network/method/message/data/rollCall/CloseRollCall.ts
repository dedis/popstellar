import {
  Hash, Timestamp, PublicKey, Lao,
} from 'model/objects';
import { OpenedLaoStore } from 'store';
import { ProtocolError } from 'model/network/ProtocolError';
import { validateDataObject } from 'model/network/validation';
import { ActionType, MessageData, ObjectType } from '../MessageData';
import { checkTimestampStaleness, checkAttendees } from '../Checker';

export class CloseRollCall implements MessageData {
  public readonly object: ObjectType = ObjectType.ROLL_CALL;

  public readonly action: ActionType = ActionType.CLOSE;

  public readonly id: Hash;

  public readonly start: Timestamp;

  public readonly end: Timestamp;

  public readonly attendees: PublicKey[];

  constructor(msg: Partial<CloseRollCall>) {
    if (!msg.start) throw new ProtocolError('Undefined \'start\' parameter encountered during \'CloseRollCall\'');
    checkTimestampStaleness(msg.start);
    this.start = msg.start;

    if (!msg.end) throw new ProtocolError('Undefined \'end\' parameter encountered during \'CloseRollCall\'');
    if (msg.end < msg.start) throw new ProtocolError('Invalid timestamp encountered: \'end\' parameter smaller than \'start\'');
    this.end = msg.end;

    if (!msg.attendees) throw new ProtocolError('Undefined \'attendees\' parameter encountered during \'CloseRollCall\'');
    checkAttendees(msg.attendees);
    this.attendees = [...msg.attendees];

    if (!msg.id) throw new ProtocolError('Undefined \'id\' parameter encountered during \'CloseRollCall\'');

    // FIXME: implementation not finished, get event from storage,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const lao: Lao = OpenedLaoStore.get();
    /*
    const expectedHash = Hash.fromStringArray(
      EventTags.ROLL_CALL, lao.id.toString(), lao.creation.toString(), lao.name
    );
    if (!expectedHash.equals(msg.id))
      throw new ProtocolError(
        'Invalid \'id\' parameter encountered during \'CloseRollCall\': unexpected id value
      '); */
    this.id = msg.id;
  }

  public static fromJson(obj: any): CloseRollCall {
    const { errors } = validateDataObject(ObjectType.ROLL_CALL, ActionType.CLOSE, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid close roll call data message\n\n${errors}`);
    }

    return new CloseRollCall({
      ...obj,
      start: new Timestamp(obj.start),
      end: new Timestamp(obj.end),
      attendees: obj.attendees.map((key: string) => new PublicKey(key)),
      id: new Hash(obj.id),
    });
  }
}
