import {Base64Data, Hash, Lao, PublicKey, Signature, Timestamp} from "Model/Objects";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { WitnessSignature } from "Model/Objects/WitnessSignature";
import { ProtocolError } from "../../../../ProtocolError";
import { checkModificationId, checkModificationSignatures, checkTimestampStaleness } from "../checker";
import { OpenedLaoStore } from 'Store';
import { eventTags } from "../../../../../../websockets/WebsocketUtils";

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

    if (!msg.name)
      throw new ProtocolError('Undefined \'name\' parameter encountered during \'StateMeeting\'');
    this.name = msg.name;

    if (!msg.creation)
      throw new ProtocolError('Undefined \'creation\' parameter encountered during \'StateMeeting\'');
    checkTimestampStaleness(msg.creation);
    this.creation = new Timestamp(msg.creation.toString());

    if (!msg.last_modified)
      throw new ProtocolError('Undefined \'last_modified\' parameter encountered during \'StateMeeting\'');
    if (msg.last_modified < msg.creation)
      throw new ProtocolError('Invalid timestamp encountered: \'last_modified\' parameter smaller than \'creation\'');
    this.last_modified = new Timestamp(msg.last_modified.toString());

    if (msg.location) this.location = msg.location;

    if (!msg.start)
      throw new ProtocolError('Undefined \'start\' parameter encountered during \'StateMeeting\'');
    checkTimestampStaleness(msg.start);
    this.start = new Timestamp(msg.start.toString());

    if (msg.end) {
      if (msg.end < msg.creation)
        throw new ProtocolError('Invalid timestamp encountered: \'end\' parameter smaller than \'creation\'');
      if (msg.end < msg.start)
        throw new ProtocolError('Invalid timestamp encountered: \'end\' parameter smaller than \'start\'');
      this.end = new Timestamp(msg.end.toString());
    }

    if (msg.extra) this.extra = JSON.parse(JSON.stringify(msg.extra)); // clone JS object extra

    if (!msg.modification_id)
      throw new ProtocolError('Undefined \'modification_id\' parameter encountered during \'StateMeeting\'');
    checkModificationId(msg.modification_id);
    this.modification_id = new Hash(msg.modification_id.toString());

    if (!msg.modification_signatures)
      throw new ProtocolError('Undefined \'modification_signatures\' parameter encountered during \'StateMeeting\'');
    checkModificationSignatures(msg.modification_signatures);
    this.modification_signatures = msg.modification_signatures.map((ws) =>
      new WitnessSignature({
        witness: new PublicKey(ws.witness.toString()),
        signature: new Signature(ws.signature.toString()),
      })
    );

    if (!msg.id) throw new ProtocolError('Undefined \'id\' parameter encountered during \'StateMeeting\'');
    const lao: Lao = OpenedLaoStore.get();
    /* // FIXME get event from storage
    const expectedHash = Hash.fromStringArray(eventTags.MEETING, lao.id.toString(), lao.creation.toString(), MEETING_NAME);
    if (!expectedHash.equals(msg.id))
      throw new ProtocolError('Invalid \'id\' parameter encountered during \'StateMeeting\': unexpected id value'); */
    this.id = new Hash(msg.id.toString());
  }

  public static fromJson(obj: any): StateMeeting {

    // FIXME add JsonSchema validation to all "fromJson"
    let correctness = true;

    return correctness
    ? new StateMeeting(obj)
    : (() => { throw new ProtocolError("add JsonSchema error message"); })();
  }
}
