import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { checkTimestampStaleness } from 'core/network/validation/Checker';
import { EventTags, Hash, ProtocolError, Timestamp } from 'core/objects';

/** Data sent to create a Roll-Call event */
export class CreateRollCall implements MessageData {
  public readonly object: ObjectType = ObjectType.ROLL_CALL;

  public readonly action: ActionType = ActionType.CREATE;

  public readonly id: Hash;

  public readonly name: string;

  public readonly creation: Timestamp;

  public readonly proposed_start: Timestamp;

  public readonly proposed_end: Timestamp;

  public readonly location: string;

  public readonly description?: string;

  constructor(msg: Partial<CreateRollCall>, laoId: Hash) {
    if (!msg.name) {
      throw new ProtocolError("Undefined 'name' parameter encountered during 'CreateRollCall'");
    }
    this.name = msg.name;

    if (!msg.creation) {
      throw new ProtocolError("Undefined 'creation' parameter encountered during 'CreateRollCall'");
    }
    checkTimestampStaleness(msg.creation);
    this.creation = msg.creation;

    if (!msg.proposed_start) {
      throw new ProtocolError(
        "Undefined 'proposed_start' parameter encountered during 'CreateRollCall'",
      );
    } else if (msg.proposed_start < msg.creation) {
      throw new ProtocolError(
        "Invalid timestamp encountered: 'proposed_start' parameter smaller than 'creation'",
      );
    }
    checkTimestampStaleness(msg.proposed_start);
    this.proposed_start = msg.proposed_start;

    if (!msg.proposed_end) {
      throw new ProtocolError(
        "Undefined 'proposed_end' parameter encountered during 'CreateRollCall'",
      );
    } else if (msg.proposed_end < msg.proposed_start) {
      throw new ProtocolError(
        'Invalid timestamp encountered:' +
          " 'proposed_end' parameter smaller than 'proposed_start'",
      );
    }
    checkTimestampStaleness(msg.proposed_end);
    this.proposed_end = msg.proposed_end;

    if (!msg.location) {
      throw new ProtocolError("Undefined 'location' parameter encountered during 'CreateRollCall'");
    }
    this.location = msg.location;

    if (msg.description) {
      this.description = msg.description;
    }

    if (!msg.id) {
      throw new ProtocolError("Undefined 'id' parameter encountered during 'CreateRollCall'");
    }

    const expectedId = CreateRollCall.computeRollCallId(laoId, this.creation, this.name);

    if (!expectedId.equals(msg.id)) {
      throw new ProtocolError(
        "Invalid 'id' parameter encountered during 'CreateRollCall':" +
          ' re-computing the value yields a different result (' +
          `(expected: '${expectedId}', actual: '${msg.id}')`,
      );
    }
    this.id = msg.id;
  }

  /**
   * Computes the id of a new roll call
   * @param laoId The id of the lao this roll call is created in
   * @param createdAt The time this roll call is created
   * @param name The name of the roll call
   * @returns The id of the roll call
   */
  public static computeRollCallId(laoId: Hash, createdAt: Timestamp, name: string) {
    return Hash.fromArray(EventTags.ROLL_CALL, laoId, createdAt, name);
  }

  /**
   * Creates a CreateRollCall object from a given object.
   *
   * @param obj The parsed json data
   * @param laoId The id of the lao this roll call belongs to
   */
  public static fromJson(obj: any, laoId?: Hash): CreateRollCall {
    if (!laoId) {
      throw new Error(
        "Tried build a 'ReopenRollCall' message without knowing the associated lao id",
      );
    }

    const { errors } = validateDataObject(ObjectType.ROLL_CALL, ActionType.CREATE, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid create roll call\n\n${errors}`);
    }

    return new CreateRollCall(
      {
        ...obj,
        creation: new Timestamp(obj.creation),
        proposed_start: new Timestamp(obj.proposed_start),
        proposed_end: new Timestamp(obj.proposed_end),
        id: new Hash(obj.id),
      },
      laoId,
    );
  }
}
