import { OpenedLaoStore } from 'store';
import {
  Hash, Timestamp, Lao, EventTags,
} from 'model/objects';
import { ProtocolError } from 'model/network/ProtocolError';
import { validateDataObject } from 'model/network/validation';
import { ActionType, MessageData, ObjectType } from '../MessageData';
import { checkTimestampStaleness } from '../Checker';

export class CreateRollCall implements MessageData {
  public readonly object: ObjectType = ObjectType.ROLL_CALL;

  public readonly action: ActionType = ActionType.CREATE;

  public readonly id: Hash;

  public readonly name: string;

  public readonly creation: Timestamp;

  public readonly proposedStart: Timestamp;

  public readonly proposedEnd: Timestamp;

  public readonly location: string;

  public readonly description?: string;

  constructor(msg: Partial<CreateRollCall>) {
    if (!msg.name) {
      throw new ProtocolError("Undefined 'name' parameter encountered during 'CreateRollCall'");
    }
    this.name = msg.name;

    if (!msg.creation) {
      throw new ProtocolError("Undefined 'creation' parameter encountered during 'CreateRollCall'");
    }
    checkTimestampStaleness(msg.creation);
    this.creation = msg.creation;

    if (!msg.proposedStart) {
      throw new ProtocolError("Undefined 'proposedStart' parameter encountered during 'CreateRollCall'");
    } else if (msg.proposedStart < msg.creation) {
      throw new ProtocolError('Invalid timestamp encountered:'
          + " 'proposed_start' parameter smaller than 'creation'");
    }
    checkTimestampStaleness(msg.proposedStart);
    this.proposedStart = msg.proposedStart;

    if (!msg.proposedEnd) {
      throw new ProtocolError("Undefined 'proposedEnd' parameter encountered during 'CreateRollCall'");
    } else if (msg.proposedEnd < msg.proposedStart) {
      throw new ProtocolError('Invalid timestamp encountered:'
        + " 'proposedEnd' parameter smaller than 'proposedStart'");
    }
    checkTimestampStaleness(msg.proposedEnd);
    this.proposedEnd = msg.proposedEnd;

    if (!msg.location) {
      throw new ProtocolError("Undefined 'location' parameter encountered during 'CreateRollCall'");
    }
    this.location = msg.location;

    if (msg.description) { this.description = msg.description; }

    if (!msg.id) {
      throw new ProtocolError("Undefined 'id' parameter encountered during 'CreateRollCall'");
    }
    const lao: Lao = OpenedLaoStore.get();
    const expectedHash = Hash.fromStringArray(
      EventTags.ROLL_CALL, lao.id.toString(), lao.creation.toString(), msg.name,
    );
    if (!expectedHash.equals(msg.id)) {
      throw new ProtocolError("Invalid 'id' parameter encountered during 'CreateRollCall':"
        + ' re-computing the value yields a different result');
    }
    this.id = msg.id;
  }

  public static fromJson(obj: any): CreateRollCall {
    const { errors } = validateDataObject(ObjectType.ROLL_CALL, ActionType.CREATE, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid create roll call\n\n${errors}`);
    }

    return new CreateRollCall({
      ...obj,
      creation: new Timestamp(obj.creation),
      proposedStart: new Timestamp(obj.proposed_start),
      proposedEnd: new Timestamp(obj.proposed_end),
      id: new Hash(obj.id),
    });
  }
}
