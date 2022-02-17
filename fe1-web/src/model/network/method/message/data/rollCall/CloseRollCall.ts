import {
  Hash, Timestamp, PublicKey, Lao, EventTags,
} from 'model/objects';
import { OpenedLaoStore } from 'store';
import { ProtocolError } from 'model/network/ProtocolError';
import { validateDataObject } from 'model/network/validation';
import { ActionType, MessageData, ObjectType } from '../MessageData';
import { checkTimestampStaleness, checkAttendees } from '../Checker';

/** Data sent to close a Roll-Call event */
export class CloseRollCall implements MessageData {
  public readonly object: ObjectType = ObjectType.ROLL_CALL;

  public readonly action: ActionType = ActionType.CLOSE;

  public readonly update_id: Hash;

  public readonly closes: Hash;

  public readonly closed_at: Timestamp;

  public readonly attendees: PublicKey[];

  constructor(msg: Partial<CloseRollCall>) {
    if (!msg.closed_at) {
      throw new ProtocolError("Undefined 'closed_at' parameter encountered during 'CloseRollCall'");
    }
    checkTimestampStaleness(msg.closed_at);
    this.closed_at = msg.closed_at;

    if (!msg.attendees) {
      throw new ProtocolError("Undefined 'attendees' parameter encountered during 'CloseRollCall'");
    }
    checkAttendees(msg.attendees);
    this.attendees = [...msg.attendees];

    if (!msg.closes) {
      throw new ProtocolError("Undefined 'closes' parameter encountered during 'CloseRollCall'");
    }
    this.closes = msg.closes;

    if (!msg.update_id) {
      throw new ProtocolError("Undefined 'update_id' parameter encountered during 'CloseRollCall'");
    }
    const lao: Lao = OpenedLaoStore.get();
    const expectedHash = Hash.fromStringArray(
      EventTags.ROLL_CALL, lao.id.toString(), this.closes.toString(), this.closed_at.toString(),
    );
    if (!expectedHash.equals(msg.update_id)) {
      throw new ProtocolError("Invalid 'update_id' parameter encountered during 'CloseRollCall':"
        + ' re-computing the value yields a different result');
    }
    this.update_id = msg.update_id;
  }

  /**
   * Creates a CloseRollCall object from a given object
   * @param obj
   */
  public static fromJson(obj: any): CloseRollCall {
    const { errors } = validateDataObject(ObjectType.ROLL_CALL, ActionType.CLOSE, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid close roll call data message\n\n${errors}`);
    }

    return new CloseRollCall({
      ...obj,
      closed_at: new Timestamp(obj.closed_at),
      attendees: obj.attendees.map((key: string) => new PublicKey(key)),
      update_id: new Hash(obj.update_id),
      closes: new Hash(obj.closes),
    });
  }
}
