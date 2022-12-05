import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { checkAttendees, checkTimestampStaleness } from 'core/network/validation/Checker';
import { EventTags, Hash, ProtocolError, PublicKey, Timestamp } from 'core/objects';

/** Data sent to close a Roll-Call event */
export class CloseRollCall implements MessageData {
  public readonly object: ObjectType = ObjectType.ROLL_CALL;

  public readonly action: ActionType = ActionType.CLOSE;

  public readonly update_id: Hash;

  public readonly closes: Hash;

  public readonly closed_at: Timestamp;

  public readonly attendees: PublicKey[];

  constructor(msg: Partial<CloseRollCall>, laoId: Hash) {
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

    const expectedId = CloseRollCall.computeCloseRollCallId(laoId, this.closes, this.closed_at);

    if (!expectedId.equals(msg.update_id)) {
      throw new ProtocolError(
        "Invalid 'update_id' parameter encountered during 'CloseRollCall':" +
          ' re-computing the value yields a different result (' +
          `(expected: '${expectedId}', actual: '${msg.update_id}')`,
      );
    }
    this.update_id = msg.update_id;
  }

  /**
   * Computes the id for a closed roll call
   * @param laoId The id of the lao this roll call takes place in
   * @param rollCallToCloseId The id of the roll call that is closed with this message
   * @param closedAt The time the roll call is closed
   * @returns The id for the closed roll call
   */
  public static computeCloseRollCallId(laoId: Hash, rollCallToCloseId: Hash, closedAt: Timestamp) {
    return Hash.fromArray(EventTags.ROLL_CALL, laoId, rollCallToCloseId, closedAt);
  }

  /**
   * Creates a CloseRollCall object from a given object.
   *
   * @param obj The parsed json data
   * @param laoId The id of the lao this roll call belongs to
   */
  public static fromJson(obj: any, laoId?: Hash): CloseRollCall {
    if (!laoId) {
      throw new Error(
        "Tried build a 'ReopenRollCall' message without knowing the associated lao id",
      );
    }

    const { errors } = validateDataObject(ObjectType.ROLL_CALL, ActionType.CLOSE, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid close roll call data message\n\n${errors}`);
    }

    return new CloseRollCall(
      {
        ...obj,
        closed_at: new Timestamp(obj.closed_at),
        attendees: obj.attendees.map((key: string) => new PublicKey(key)),
        update_id: new Hash(obj.update_id),
        closes: new Hash(obj.closes),
      },
      laoId,
    );
  }
}
