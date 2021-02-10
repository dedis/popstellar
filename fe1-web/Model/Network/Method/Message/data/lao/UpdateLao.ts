import { Hash } from "Model/Objects/Hash";
import { PublicKey } from "Model/Objects/PublicKey";
import { Timestamp } from "Model/Objects/Timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { ProtocolError } from "../../../../ProtocolError";
import { checkTimestampStaleness, checkWitnesses } from "../checker";

export class UpdateLao implements MessageData {

    public readonly object: ObjectType = ObjectType.LAO;
    public readonly action: ActionType = ActionType.UPDATE_PROPERTIES;

    public readonly id: Hash;
    public readonly name: string;
    public readonly last_modified: Timestamp;
    public readonly witnesses: PublicKey[];

    constructor(msg: Partial<UpdateLao>) {

        if (!msg.name) throw new ProtocolError('Undefined \'name\' parameter encountered during \'UpdateLao\'');
        this.name = msg.name;

        if (!msg.last_modified) throw new ProtocolError('Undefined \'last_modified\' parameter encountered during \'UpdateLao\'');
        checkTimestampStaleness(msg.last_modified);
        this.last_modified = msg.last_modified;

        if (!msg.witnesses) throw new ProtocolError('Undefined \'witnesses\' parameter encountered during \'UpdateLao\'');
        checkWitnesses(msg.witnesses);
        this.witnesses = [...msg.witnesses];

        if (!msg.id) throw new ProtocolError('Undefined \'id\' parameter encountered during \'UpdateLao\'');
        // FIXME take info from storage
        /*const expectedHash = Hash.fromStringArray(msg.organizer.toString(), msg.creation.toString(), msg.name);
        if (expectedHash !== msg.id)
            throw new ProtocolError('Invalid \'id\' parameter encountered during \'UpdateLao\': unexpected id value');*/
        this.id = msg.id;
    }

    public verify(): boolean {
        // to be implemented...
        return true;
    }

    public static fromJson(obj: any): UpdateLao {
        throw new Error('Not implemented');
    }
}
