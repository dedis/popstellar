import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { checkTimestampStaleness, checkWitnesses } from 'core/network/validation/Checker';
import { Hash, ProtocolError, PublicKey, Timestamp } from 'core/objects';

/** Data sent to create a Lao */
export class CreateLao implements MessageData {
  public readonly object: ObjectType = ObjectType.LAO;

  public readonly action: ActionType = ActionType.CREATE;

  public readonly id: Hash;

  public readonly name: string;

  public readonly creation: Timestamp;

  public readonly organizer: PublicKey;

  public readonly witnesses: PublicKey[];

  constructor(msg: Partial<CreateLao>) {
    if (!msg.name) {
      throw new ProtocolError("Undefined 'name' parameter encountered during 'CreateLao'");
    }
    this.name = msg.name;

    if (!msg.creation) {
      throw new ProtocolError("Undefined 'creation' parameter encountered during 'CreateLao'");
    }
    checkTimestampStaleness(msg.creation);
    this.creation = msg.creation;

    if (!msg.organizer) {
      throw new ProtocolError("Undefined 'organizer' parameter encountered during 'CreateLao'");
    }
    this.organizer = msg.organizer;

    if (!msg.witnesses) {
      throw new ProtocolError("Undefined 'witnesses' parameter encountered during 'CreateLao'");
    }
    checkWitnesses(msg.witnesses);
    this.witnesses = [...msg.witnesses];

    if (!msg.id) {
      throw new ProtocolError("Undefined 'id' parameter encountered during 'CreateLao'");
    }
    const expectedHash: Hash = Hash.fromStringArray(
      msg.organizer,
      msg.creation.toString(),
      msg.name,
    );
    if (!expectedHash.equals(msg.id)) {
      throw new ProtocolError(
        "Invalid 'id' parameter encountered during 'CreateLao': unexpected id value",
      );
    }
    this.id = msg.id;
  }

  /**
   * Creates a CreateLao object from a given object
   * @param obj
   */
  public static fromJson(obj: any): CreateLao {
    const { errors } = validateDataObject(ObjectType.LAO, ActionType.CREATE, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid LAO create\n\n${errors}`);
    }

    return new CreateLao({
      ...obj,
      creation: new Timestamp(obj.creation),
      organizer: new PublicKey(obj.organizer),
      witnesses: obj.witnesses.map((key: string) => new PublicKey(key)),
      id: new Hash(obj.id),
    });
  }
}
