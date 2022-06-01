import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { checkTimestampStaleness } from 'core/network/validation/Checker';
import { EventTags, Hash, ProtocolError, Timestamp } from 'core/objects';

/** Data sent to create a Meeting event */
export class CreateMeeting implements MessageData {
  public readonly object: ObjectType = ObjectType.MEETING;

  public readonly action: ActionType = ActionType.CREATE;

  public readonly id: Hash;

  public readonly name: string;

  public readonly creation: Timestamp;

  public readonly location?: string;

  public readonly start: Timestamp;

  public readonly end?: Timestamp;

  public readonly extra?: {};

  constructor(msg: Partial<CreateMeeting>, laoId: Hash) {
    if (!msg.name) {
      throw new ProtocolError("Undefined 'name' parameter encountered during 'CreateMeeting'");
    }
    this.name = msg.name;

    if (!msg.creation) {
      throw new ProtocolError("Undefined 'creation' parameter encountered during 'CreateMeeting'");
    }
    checkTimestampStaleness(msg.creation);
    this.creation = msg.creation;

    if (msg.location) {
      this.location = msg.location;
    }

    if (!msg.start) {
      throw new ProtocolError("Undefined 'start' parameter encountered during 'CreateMeeting'");
    }
    checkTimestampStaleness(msg.start);
    this.start = msg.start;

    if (msg.end) {
      if (msg.end < msg.creation) {
        throw new ProtocolError(
          "Invalid timestamp encountered: 'end' parameter smaller than 'creation'",
        );
      }
      this.end = msg.end;
    }

    if (msg.extra) {
      this.extra = JSON.parse(JSON.stringify(msg.extra));
    } // clone JS object extra

    if (!msg.id) {
      throw new ProtocolError("Undefined 'id' parameter encountered during 'CreateMeeting'");
    }

    const expectedId = CreateMeeting.computeMeetingId(laoId, this.creation, this.name);
    if (!expectedId.equals(msg.id)) {
      throw new ProtocolError(
        "Invalid 'id' parameter encountered during 'CreateMeeting':" +
          ' re-computing the value yields a different result (' +
          `(expected: '${expectedId}', actual: '${msg.id}')`,
      );
    }

    this.id = msg.id;
  }

  /**
   * Computes the id of a meeting
   *
   * @param laoId - The ID of the LAO this message was sent to
   * @param createdAt - The time the meeting was created
   * @param name The name of the meeting
   */
  public static computeMeetingId(laoId: Hash, createdAt: Timestamp, name: string) {
    return Hash.fromStringArray(EventTags.MEETING, laoId.toString(), createdAt.toString(), name);
  }

  /**
   * Creates a CreateMeeting object from a given object.
   *
   * @param obj The parsed json data
   * @param laoId The id of the lao this message belongs to
   */
  public static fromJson(obj: any, laoId?: Hash): CreateMeeting {
    if (!laoId) {
      throw new Error(
        "Tried build a 'CreateMeeting' message without knowing the associated lao id",
      );
    }

    const { errors } = validateDataObject(ObjectType.MEETING, ActionType.CREATE, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid meeting create\n\n${errors}`);
    }

    return new CreateMeeting(
      {
        ...obj,
        creation: new Timestamp(obj.creation),
        start: new Timestamp(obj.start),
        end: obj.end !== undefined ? new Timestamp(obj.end) : undefined,
        id: new Hash(obj.id),
      },
      laoId,
    );
  }
}
