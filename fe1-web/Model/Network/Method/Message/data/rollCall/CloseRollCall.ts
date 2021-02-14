import { Hash } from "Model/Objects/Hash";
import { Timestamp } from "Model/Objects/Timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { PublicKey } from "Model/Objects/PublicKey";
import { ProtocolError} from "../../../../ProtocolError";
import { checkTimestampStaleness, checkAttendees } from "../checker";
import {getStorageCurrentLao} from "../../../../../../Store/Storage";
import {Lao} from "../../../../../Objects";
import {eventTags} from "../../../../../../websockets/WebsocketUtils";

export class CloseRollCall implements MessageData {

  public readonly object: ObjectType = ObjectType.ROLL_CALL;
  public readonly action: ActionType = ActionType.CLOSE;

  public readonly id: Hash;
  public readonly start: Timestamp;
  public readonly end: Timestamp;
  public readonly attendees: PublicKey[];

  constructor(msg: Partial<CloseRollCall>) {

    if (!msg.start) throw new ProtocolError('Undefined \'start\' parameter encountered during \'CloseRollCall\'');
    checkTimestampStaleness(msg.start);
    this.start = new Timestamp(msg.start.toString());

    if (!msg.end) throw new ProtocolError('Undefined \'end\' parameter encountered during \'CloseRollCall\'');
    if (msg.end < msg.start)
      throw new ProtocolError('Invalid timestamp encountered: \'end\' parameter smaller than \'start\'');
    this.end = new Timestamp(msg.end.toString());

    if (!msg.attendees) throw new ProtocolError('Undefined \'attendees\' parameter encountered during \'CloseRollCall\'');
    checkAttendees(msg.attendees);
    this.attendees = msg.attendees.map((key) => new PublicKey(key.toString()));

    if (!msg.id) throw new ProtocolError('Undefined \'id\' parameter encountered during \'CloseRollCall\'');
    const lao: Lao = getStorageCurrentLao().getCurrentLao();
    /* // FIXME get event from storage
    const expectedHash = Hash.fromStringArray(eventTags.ROLL_CALL, lao.id.toString(), lao.creation.toString(), lao.name);
    if (!expectedHash.equals(msg.id))
      throw new ProtocolError('Invalid \'id\' parameter encountered during \'CloseRollCall\': unexpected id value');*/
    this.id = new Hash(msg.id.toString());
  }

  public static fromJson(obj: any): CloseRollCall {

    // FIXME add JsonSchema validation to all "fromJson"
    let correctness = true;

    return correctness
    ? new CloseRollCall(obj)
    : (() => { throw new ProtocolError("add JsonSchema error message"); })();
  }
}
