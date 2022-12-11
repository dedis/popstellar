import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { checkTimestampStaleness, checkWitnessSignatures } from 'core/network/validation/Checker';
import {
  Hash,
  ProtocolError,
  PublicKey,
  Signature,
  Timestamp,
  WitnessSignature,
} from 'core/objects';

/** Data received to track the state of a Meeting */
export class StateMeeting implements MessageData {
  public readonly object: ObjectType = ObjectType.MEETING;

  public readonly action: ActionType = ActionType.STATE;

  public readonly id: Hash;

  public readonly name: string;

  public readonly creation: Timestamp;

  public readonly last_modified: Timestamp;

  public readonly location?: string;

  public readonly start: Timestamp;

  public readonly end?: Timestamp;

  public readonly extra?: {};

  public readonly modification_id: Hash;

  public readonly modification_signatures: WitnessSignature[];

  constructor(msg: Partial<StateMeeting>) {
    if (!msg.name) {
      throw new ProtocolError("Undefined 'name' parameter encountered during 'StateMeeting'");
    }
    this.name = msg.name;

    if (!msg.creation) {
      throw new ProtocolError("Undefined 'creation' parameter encountered during 'StateMeeting'");
    }
    checkTimestampStaleness(msg.creation);
    this.creation = msg.creation;

    if (!msg.last_modified) {
      throw new ProtocolError(
        "Undefined 'last_modified' parameter encountered during 'StateMeeting'",
      );
    }
    if (msg.last_modified < msg.creation) {
      throw new ProtocolError(
        "Invalid timestamp encountered: 'last_modified' parameter smaller than 'creation'",
      );
    }
    this.last_modified = msg.last_modified;

    if (msg.location) {
      this.location = msg.location;
    }

    if (!msg.start) {
      throw new ProtocolError("Undefined 'start' parameter encountered during 'StateMeeting'");
    }
    checkTimestampStaleness(msg.start);
    this.start = msg.start;

    if (msg.end) {
      if (msg.end < msg.creation) {
        throw new ProtocolError(
          "Invalid timestamp encountered: 'end' parameter smaller than 'creation'",
        );
      }
      if (msg.end < msg.start) {
        throw new ProtocolError(
          "Invalid timestamp encountered: 'end' parameter smaller than 'start'",
        );
      }
      this.end = msg.end;
    }

    if (msg.extra) {
      this.extra = JSON.parse(JSON.stringify(msg.extra));
    } // clone JS object extra

    if (!msg.modification_id) {
      throw new ProtocolError(
        "Undefined 'modification_id' parameter encountered during 'StateMeeting'",
      );
    }
    // TODO: checkModificationId(msg.modification_id);
    this.modification_id = msg.modification_id;

    if (!msg.modification_signatures) {
      throw new ProtocolError(
        "Undefined 'modification_signatures' parameter encountered during 'StateMeeting'",
      );
    }
    checkWitnessSignatures(msg.modification_signatures, msg.modification_id);
    this.modification_signatures = [...msg.modification_signatures];

    if (!msg.id) {
      throw new ProtocolError("Undefined 'id' parameter encountered during 'StateMeeting'");
    }

    // FIXME: implementation not finished, get event from storage,
    /*
    const lao: Lao = OpenedLaoStore.get();
    const expectedHash = Hash.fromArray(
      EventTags.MEETING, lao.id, lao.creation, MEETING_NAME,
    );
    if (!expectedHash.equals(msg.id))
      throw new ProtocolError(
        'Invalid \'id\' parameter encountered during \'StateMeeting\': unexpected id value'
      ); */
    this.id = msg.id;
  }

  /**
   * Creates a StateMeeting object from a given object.
   *
   * @param obj
   */
  public static fromJson(obj: any): StateMeeting {
    const { errors } = validateDataObject(ObjectType.MEETING, ActionType.STATE, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid meeting state\n\n${errors}`);
    }

    return new StateMeeting({
      ...obj,
      creation: new Timestamp(obj.creation),
      last_modified: new Timestamp(obj.last_modified),
      start: new Timestamp(obj.start),
      end: obj.end !== undefined ? new Timestamp(obj.end) : undefined,
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
