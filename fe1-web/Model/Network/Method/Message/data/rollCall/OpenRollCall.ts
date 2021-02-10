import { Hash } from "Model/Objects/hash";
import { Timestamp } from "Model/Objects/timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { ProtocolError } from "../../../../ProtocolError";
import { checkTimestampStaleness } from "../checker";

export class OpenRollCall implements MessageData {

    public readonly object: ObjectType = ObjectType.ROLL_CALL;
    public readonly action: ActionType; // could be open or reopen

    public readonly id: Hash;
    public readonly start: Timestamp;

    constructor(msg: Partial<OpenRollCall>) {

        if (!msg.action) throw new ProtocolError('Undefined \'action\' parameter encountered during \'OpenRollCall\'');
        if (msg.action !== ActionType.OPEN && msg.action !== ActionType.REOPEN)
            throw new ProtocolError('Invalid \'action\' parameter encountered during \'OpenRollCall\'');
        this.action = msg.action;

        if (!msg.start) throw new ProtocolError('Undefined \'start\' parameter encountered during \'OpenRollCall\'');
        checkTimestampStaleness(msg.start);
        this.start = msg.start;

        if (!msg.id) throw new ProtocolError('Undefined \'id\' parameter encountered during \'CreateLao\'');
        // FIXME take info from storage
        /*const expectedHash = Hash.fromStringArray(eventTags.ROLL_CALL, LAO_ID, CREATION, NAME);
        if (expectedHash !== msg.id)
            throw new ProtocolError('Invalid \'id\' parameter encountered during \'CreateLao\': unexpected id value');*/
        this.id = msg.id;
    }

    public verify(): boolean {
        // to be implemented...
        return true;
    }

    public static fromJson(obj: any): OpenRollCall {
        throw new Error('Not implemented');
    }
}
