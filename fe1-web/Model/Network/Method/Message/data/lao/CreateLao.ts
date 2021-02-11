import { Hash, PublicKey, Timestamp } from "Model/Objects";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { checkTimestampStaleness, checkWitnesses } from '../checker'
import { ProtocolError } from "../../../../ProtocolError";

export class CreateLao implements MessageData {

  public readonly object: ObjectType = ObjectType.LAO;
  public readonly action: ActionType = ActionType.CREATE;

  public readonly id: Hash;
  public readonly name: string;
  public readonly creation: Timestamp;
  public readonly organizer: PublicKey;
  public readonly witnesses: PublicKey[];

  constructor(msg: Partial<CreateLao>) {

    if (!msg.name) throw new ProtocolError('Undefined \'name\' parameter encountered during \'CreateLao\'');
    this.name = msg.name;

    if (!msg.creation) throw new ProtocolError('Undefined \'creation\' parameter encountered during \'CreateLao\'');
    checkTimestampStaleness(msg.creation);
    this.creation = msg.creation;

    if (!msg.organizer) throw new ProtocolError('Undefined \'organizer\' parameter encountered during \'CreateLao\'');
    this.organizer = msg.organizer;

    if (!msg.witnesses) throw new ProtocolError('Undefined \'witnesses\' parameter encountered during \'CreateLao\'');
    checkWitnesses(msg.witnesses);
    this.witnesses = [...msg.witnesses];

    if (!msg.id) throw new ProtocolError('Undefined \'id\' parameter encountered during \'CreateLao\'');
    const expectedHash: Hash = Hash.fromStringArray(msg.organizer.toString(), msg.creation.toString(), msg.name);
    if (!expectedHash.equals(msg.id))
      throw new ProtocolError('Invalid \'id\' parameter encountered during \'CreateLao\': unexpected id value');
    this.id = msg.id;
  }

  public static fromJson(obj: any): CreateLao {

    // FIXME add JsonSchema validation to all "fromJson"
    let correctness = true;

    return correctness
      ? new CreateLao(obj)
      : (() => { throw new ProtocolError("add JsonSchema error message"); })();
  }
}
