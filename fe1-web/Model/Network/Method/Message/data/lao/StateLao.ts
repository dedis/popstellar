import { Hash } from "Model/Objects/Hash";
import { PublicKey } from "Model/Objects/PublicKey";
import { Timestamp } from "Model/Objects/Timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { WitnessSignature } from "Model/Objects/WitnessSignature";
import { ProtocolError } from "../../../../ProtocolError";
import { checkTimestampStaleness, checkWitnesses, checkModificationId, checkModificationSignatures } from "../checker";

export class StateLao implements MessageData {

  public readonly object: ObjectType = ObjectType.LAO;
  public readonly action: ActionType = ActionType.STATE;

  public readonly id: Hash;
  public readonly name: string;
  public readonly creation: Timestamp;
  public readonly last_modified: Timestamp;
  public readonly organizer: PublicKey;
  public readonly witnesses: PublicKey[];
  public readonly modification_id: Hash;
  public readonly modification_signatures: WitnessSignature[];

  constructor(msg: Partial<StateLao>) {

    if (!msg.name) throw new ProtocolError('Undefined \'name\' parameter encountered during \'StateLao\'');
    this.name = msg.name;

    if (!msg.creation) throw new ProtocolError('Undefined \'creation\' parameter encountered during \'StateLao\'');
    checkTimestampStaleness(msg.creation);
    this.creation = msg.creation;

    if (!msg.last_modified) throw new ProtocolError('Undefined \'last_modified\' parameter encountered during \'StateLao\'');
    if (msg.last_modified < msg.creation)
      throw new ProtocolError('Invalid timestamp encountered: \'last_modified\' parameter smaller than \'creation\'');
    this.last_modified = msg.last_modified;

    if (!msg.organizer) throw new ProtocolError('Undefined \'organizer\' parameter encountered during \'StateLao\'');
    this.organizer = msg.organizer;

    if (!msg.witnesses) throw new ProtocolError('Undefined \'witnesses\' parameter encountered during \'StateLao\'');
    checkWitnesses(msg.witnesses);
    this.witnesses = [...msg.witnesses];

    if (!msg.modification_id)
      throw new ProtocolError('Undefined \'modification_id\' parameter encountered during \'StateLao\'');
    checkModificationId(msg.modification_id);
    this.modification_id = msg.modification_id;

    if (!msg.modification_signatures)
      throw new ProtocolError('Undefined \'modification_signatures\' parameter encountered during \'StateLao\'');
    checkModificationSignatures(msg.modification_signatures);
    this.modification_signatures = [...msg.modification_signatures];

    if (!msg.id) throw new ProtocolError('Undefined \'id\' parameter encountered during \'StateLao\'');
    const expectedHash = Hash.fromStringArray(msg.organizer.toString(), msg.creation.toString(), msg.name);
    if (!expectedHash.equals(msg.id))
      throw new ProtocolError('Invalid \'id\' parameter encountered during \'StateLao\': unexpected id value');
    this.id = msg.id;
  }

  public static fromJson(obj: any): StateLao {

    // FIXME add JsonSchema validation to all "fromJson"
    let correctness = true;

    return correctness
      ? new StateLao(obj)
      : (() => { throw new ProtocolError("add JsonSchema error message"); })();
  }
}
