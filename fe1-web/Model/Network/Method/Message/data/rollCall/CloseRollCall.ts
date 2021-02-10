import { Hash } from "Model/Objects/Hash";
import { Timestamp } from "Model/Objects/Timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { PublicKey } from "Model/Objects/PublicKey";
import { ProtocolError} from "../../../../ProtocolError";
import { checkTimestampStaleness, checkAttendees } from "../checker";

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
        this.start = msg.start;

        if (!msg.end) throw new ProtocolError('Undefined \'end\' parameter encountered during \'CloseRollCall\'');
        if (msg.end < msg.start)
            throw new ProtocolError('Invalid timestamp encountered: \'end\' parameter smaller than \'start\'');
        this.end = msg.end;

        if (!msg.attendees) throw new ProtocolError('Undefined \'attendees\' parameter encountered during \'CloseRollCall\'');
        checkAttendees(msg.attendees);
        this.attendees = [...msg.attendees];

        if (!msg.id) throw new ProtocolError('Undefined \'id\' parameter encountered during \'CloseRollCall\'');
        // FIXME take info from storage
        /*const expectedHash = Hash.fromStringArray(eventTags.ROLL_CALL, LAO_ID, CREATION, NAME);
        if (expectedHash !== msg.id)
            throw new ProtocolError('Invalid \'id\' parameter encountered during \'CloseRollCall\': unexpected id value');*/
        this.id = msg.id;
    }

    public verify(): boolean {
        // to be implemented...
        return true;
    }

    public static fromJson(obj: any): CloseRollCall {
        throw new Error('Not implemented');
    }
}
