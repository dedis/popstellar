import { Hash } from "Model/Objects/hash";
import { PublicKey } from "Model/Objects/publicKey";
import { Timestamp } from "Model/Objects/timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";

export class UpdateLao implements MessageData {

    public readonly object: ObjectType = ObjectType.LAO;
    public readonly action: ActionType = ActionType.UPDATE_PROPERTIES;

    public readonly id: Hash;
    public readonly name: string;
    public readonly last_modified: Timestamp;
    public readonly witnesses: PublicKey[];

    constructor(msg: Partial<UpdateLao>) {
        Object.assign(this, msg);
    }

    public verify(): boolean {
        // to be implemented...
        return true;
    }
}
