import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import {
  checkTimestampStaleness,
  checkWitnesses,
  checkWitnessSignatures,
} from 'core/network/validation/Checker';
import {
  Hash,
  ProtocolError,
  PublicKey,
  Signature,
  Timestamp,
  WitnessSignature,
} from 'core/objects';

/** Data received to track the state of a lao */
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
    const makeErr = (name: string) => `Undefined '${name}' parameter encountered during 'StateLao'`;

    if (!msg.name) {
      throw new ProtocolError(makeErr('name'));
    }
    this.name = msg.name;

    if (!msg.creation) {
      throw new ProtocolError(makeErr('creation'));
    }
    checkTimestampStaleness(msg.creation);
    this.creation = msg.creation;

    if (!msg.last_modified) {
      throw new ProtocolError(makeErr('last_modified'));
    }
    if (msg.last_modified < msg.creation) {
      throw new ProtocolError(
        "Invalid timestamp encountered:'last_modified' parameter smaller than 'creation'",
      );
    }
    this.last_modified = msg.last_modified;

    if (!msg.organizer) {
      throw new ProtocolError(makeErr('organizer'));
    }
    this.organizer = msg.organizer;

    if (!msg.witnesses) {
      throw new ProtocolError(makeErr('witnesses'));
    }
    checkWitnesses(msg.witnesses);
    this.witnesses = [...msg.witnesses];

    if (!msg.modification_id) {
      throw new ProtocolError(makeErr('modification_id'));
    }
    this.modification_id = msg.modification_id;

    if (!msg.modification_signatures) {
      throw new ProtocolError(makeErr('modification_signatures'));
    }
    checkWitnessSignatures(msg.modification_signatures, msg.modification_id);
    this.modification_signatures = [...msg.modification_signatures];

    if (!msg.id) {
      throw new ProtocolError(makeErr('id'));
    }
    const expectedHash = Hash.fromStringArray(msg.organizer, msg.creation.toString(), msg.name);
    if (!expectedHash.equals(msg.id)) {
      throw new ProtocolError("Invalid 'id' parameter encountered during 'StateLao'");
    }
    this.id = msg.id;
  }

  /**
   * Creates a StateLao object from a given object
   * @param obj
   */
  public static fromJson(obj: any): StateLao {
    const { errors } = validateDataObject(ObjectType.LAO, ActionType.STATE, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid LAO state\n\n${errors}`);
    }

    return new StateLao({
      ...obj,
      creation: new Timestamp(obj.creation),
      last_modified: new Timestamp(obj.last_modified),
      organizer: new PublicKey(obj.organizer),
      witnesses: obj.witnesses.map((key: string) => new PublicKey(key)),
      modification_id: new Hash(obj.modification_id),
      modification_signatures: obj.modification_signatures.map(
        (ws: any) =>
          new WitnessSignature({
            witness: new PublicKey(ws.witness),
            signature: new Signature(ws.signature),
          }),
      ),
      id: new Hash(obj.id),
    });
  }
}
