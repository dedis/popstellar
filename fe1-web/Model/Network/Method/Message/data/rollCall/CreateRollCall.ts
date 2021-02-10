import { Hash } from "Model/Objects/hash";
import { Timestamp } from "Model/Objects/timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { ProtocolError } from "../../../../ProtocolError";
import { checkTimestampStaleness } from "../checker";

export class CreateRollCall implements MessageData {

    public readonly object: ObjectType = ObjectType.ROLL_CALL;
    public readonly action: ActionType = ActionType.CREATE;

    public readonly id: Hash;
    public readonly name: string;
    public readonly creation: Timestamp;
    public readonly start?: Timestamp;
    public readonly scheduled?: Timestamp;
    public readonly location: string;
    public readonly roll_call_description?: string;

    constructor(msg: Partial<CreateRollCall>) {

        if (!msg.name) throw new ProtocolError('Undefined \'name\' parameter encountered during \'CreateRollCall\'');
        this.name = msg.name;

        if (!msg.creation) throw new ProtocolError('Undefined \'creation\' parameter encountered during \'CreateRollCall\'');
        checkTimestampStaleness(msg.creation);
        this.creation = msg.creation;

        // if both are present or neither
        if (msg.start === msg.scheduled)
            throw new ProtocolError('Invalid \'start\' and/or \'scheduled\' value encountered during \'CreateRollCall\'');

        if (msg.start && msg.start < msg.creation) {
            throw new ProtocolError('Invalid timestamp encountered: \'start\' parameter smaller than \'creation\'');
        } else { this.start = msg.start; }

        if (msg.scheduled && msg.scheduled < msg.creation) {
            throw new ProtocolError('Invalid timestamp encountered: \'scheduled\' parameter smaller than \'creation\'');
        } else { this.scheduled = msg.scheduled; }

        if (!msg.location) throw new ProtocolError('Undefined \'location\' parameter encountered during \'CreateRollCall\'');
        this.location = msg.location;

        if (msg.roll_call_description) this.roll_call_description = msg.roll_call_description;

        if (!msg.id) throw new ProtocolError('Undefined \'id\' parameter encountered during \'CreateRollCall\'');
        // FIXME take info from storage
        /*const expectedHash = Hash.fromStringArray(eventTags.ROLL_CALL, LAO_ID, msg.creation.toString(), msg.name);
        if (expectedHash !== msg.id)
            throw new ProtocolError('Invalid \'id\' parameter encountered during \'CreateRollCall\': unexpected id value');*/
        this.id = msg.id;
    }

    public verify(): boolean {
        // to be implemented...
        return true;
    }

    public static fromJson(obj: any): CreateRollCall {
        throw new Error('Not implemented');
    }
}
