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

  public readonly updateId: Hash;

  public readonly closes: Hash;

  public readonly closedAt: Timestamp;

  public readonly attendees: PublicKey[];

  constructor(msg: Partial<CloseRollCall>) {
    if (!msg.closedAt) {
      throw new ProtocolError("Undefined 'closedAt' parameter encountered during 'CloseRollCall'");
    }
    checkTimestampStaleness(msg.closedAt);
    this.closedAt = msg.closedAt;

    if (!msg.attendees) {
      throw new ProtocolError("Undefined 'attendees' parameter encountered during 'CloseRollCall'");
    }
    checkAttendees(msg.attendees);
    this.attendees = [...msg.attendees];

    if (!msg.updateId) {
      throw new ProtocolError("Undefined 'update_id' parameter encountered during 'CloseRollCall'");
    }

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
    this.updateId = msg.updateId;

    if (!msg.closes) {
      throw new ProtocolError("Undefined 'closes' parameter encountered during 'CloseRollCall'");
    }
    this.closes = msg.closes;
  }

  public static fromJson(obj: any): CloseRollCall {
    const { errors } = validateDataObject(ObjectType.ROLL_CALL, ActionType.CLOSE, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid close roll call data message\n\n${errors}`);
    }

    return new CloseRollCall({
      ...obj,
      closedAt: new Timestamp(obj.closed_at),
      attendees: obj.attendees.map((key: string) => new PublicKey(key)),
      updateId: new Hash(obj.update_id),
      closes: new Hash(obj.closes),
    });
  }
}
