import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { checkTimestampStaleness } from 'core/network/validation/Checker';
import { EventTags, Hash, ProtocolError, Timestamp } from 'core/objects';

const paramError = (o: OpenRollCall) => `parameter encountered during roll call ${o.action}`;

/** Data sent to open a Roll-Call event */
export class OpenRollCall implements MessageData {
  public readonly object: ObjectType = ObjectType.ROLL_CALL;

  public readonly action: ActionType = ActionType.OPEN;

  public readonly update_id: Hash;

  public readonly opens: Hash;

  public readonly opened_at: Timestamp;

  constructor(msg: Partial<OpenRollCall>, laoId: Hash) {
    if (!msg.opened_at) {
      throw new ProtocolError(`Undefined 'opened_at' ${paramError(this)}`);
    }
    checkTimestampStaleness(msg.opened_at);
    this.opened_at = msg.opened_at;

    if (!msg.opens) {
      throw new ProtocolError(`Undefined 'opens' ${paramError(this)}`);
    }
    this.opens = msg.opens;

    if (!msg.update_id) {
      throw new ProtocolError(`Undefined 'update_id' ${paramError(this)}`);
    }

    const expectedId = OpenRollCall.computeOpenRollCallId(laoId, this.opens, this.opened_at);

    if (!expectedId.equals(msg.update_id)) {
      throw new ProtocolError(
        `Invalid 'update_id' ${paramError(this)}:` +
          ' re-computing the value yields a different result (' +
          `(expected: '${expectedId}', actual: '${msg.update_id}')`,
      );
    }
    this.update_id = msg.update_id;
  }

  /**
   * Computes the id for an opened roll call
   * @param laoId The id of the lao this roll call takes place in
   * @param rollCallIdToOpen The id of the roll call that is opened with this message
   * @param openedAt The time the roll call is opened
   * @returns The id for the opened roll call
   */
  public static computeOpenRollCallId(
    laoId: Hash,
    rollCallIdToOpen: Hash,
    openedAt: Timestamp,
  ): Hash {
    return Hash.fromStringArray(
      EventTags.ROLL_CALL,
      laoId.valueOf(),
      rollCallIdToOpen.valueOf(),
      openedAt.toString(),
    );
  }

  /**
   * Creates an OpenRollCall object from a given object.
   *
   * @param obj The parsed json data
   * @param laoId The id of the lao this roll call is in
   */
  public static fromJson(obj: any, laoId?: Hash): OpenRollCall {
    if (!laoId) {
      throw new Error(
        "Tried build a 'ReopenRollCall' message without knowing the associated lao id",
      );
    }

    const { errors } = validateDataObject(ObjectType.ROLL_CALL, ActionType.OPEN, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid open roll call\n\n${errors}`);
    }

    return new OpenRollCall(
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
