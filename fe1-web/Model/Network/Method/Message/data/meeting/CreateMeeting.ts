import { Hash } from "Model/Objects/Hash";
import { Timestamp } from "Model/Objects/Timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { ProtocolError } from "../../../../ProtocolError";
import { checkTimestampStaleness } from "../checker";

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

    constructor(msg: Partial<CreateMeeting>) {

        if (!msg.name) throw new ProtocolError('Undefined \'name\' parameter encountered during \'CreateMeeting\'');
        this.name = msg.name;

        if (!msg.creation) throw new ProtocolError('Undefined \'creation\' parameter encountered during \'CreateMeeting\'');
        checkTimestampStaleness(msg.creation);
        this.creation = msg.creation;

        if (msg.location) this.location = msg.location;

        if (!msg.start) throw new ProtocolError('Undefined \'start\' parameter encountered during \'CreateMeeting\'');
        checkTimestampStaleness(msg.start);
        this.start = msg.start;

        if (msg.end) {
            if (msg.end < msg.creation)
                throw new ProtocolError('Invalid timestamp encountered: \'end\' parameter smaller than \'creation\'');
            this.end = msg.end;
        }

        if (msg.extra) this.extra = msg.extra;

        if (!msg.id) throw new ProtocolError('Undefined \'id\' parameter encountered during \'CreateMeeting\'');
        // FIXME take info from storage
        /*const expectedHash = Hash.fromStringArray(eventTags.MEETING, LAO_ID, msg.creation.toString(), msg.name);
        if (expectedHash !== msg.id)
            throw new ProtocolError('Invalid \'id\' parameter encountered during \'CreateMeeting\': unexpected id value');*/
        this.id = msg.id;
    }

    public verify(): boolean {
        // to be implemented...
        return true;
    }

    public static fromJson(obj: any): CreateMeeting {
        throw new Error('Not implemented');
    }
}
