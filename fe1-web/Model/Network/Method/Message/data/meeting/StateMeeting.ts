import { Hash } from "Model/Objects/Hash";
import { Timestamp } from "Model/Objects/Timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { WitnessSignature } from "Model/Objects/WitnessSignature";
import { ProtocolError } from "../../../../ProtocolError";
import { checkModificationId, checkModificationSignatures, checkTimestampStaleness } from "../checker";

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
        this.creation = msg.creation;

        if (!msg.last_modified)
            throw new ProtocolError('Undefined \'last_modified\' parameter encountered during \'StateMeeting\'');
        if (msg.last_modified < msg.creation)
            throw new ProtocolError('Invalid timestamp encountered: \'last_modified\' parameter smaller than \'creation\'');
        this.last_modified = msg.last_modified;

        if (msg.location) this.location = msg.location;

        if (!msg.start)
            throw new ProtocolError('Undefined \'start\' parameter encountered during \'StateMeeting\'');
        checkTimestampStaleness(msg.start);
        this.start = msg.start;

        if (msg.end) {
            if (msg.end < msg.creation)
                throw new ProtocolError('Invalid timestamp encountered: \'end\' parameter smaller than \'creation\'');
            if (msg.end < msg.start)
                throw new ProtocolError('Invalid timestamp encountered: \'end\' parameter smaller than \'start\'');
            this.end = msg.end;
        }

        if (msg.extra) this.extra = msg.extra;

        if (!msg.modification_id)
            throw new ProtocolError('Undefined \'modification_id\' parameter encountered during \'StateMeeting\'');
        checkModificationId(msg.modification_id);
        this.modification_id = msg.modification_id;

        if (!msg.modification_signatures)
            throw new ProtocolError('Undefined \'modification_signatures\' parameter encountered during \'StateMeeting\'');
        checkModificationSignatures(msg.modification_signatures);
        this.modification_signatures = [...msg.modification_signatures];

        if (!msg.id) throw new ProtocolError('Undefined \'id\' parameter encountered during \'StateMeeting\'');
        // FIXME take info from storage
        /*const expectedHash = Hash.fromStringArray(eventTags.MEETING, LAO_ID, msg.creation.toString(), msg.name);
        if (!expectedHash.equals(msg.id))
            throw new ProtocolError('Invalid \'id\' parameter encountered during \'StateMeeting\': unexpected id value');*/
        this.id = msg.id;
    }

    public static fromJson(obj: any): StateMeeting {

      // FIXME add JsonSchema validation to all "fromJson"
      let correctness = true;

      return correctness
        ? new StateMeeting(obj)
        : (() => { throw new ProtocolError("add JsonSchema error message"); })();
    }
}
