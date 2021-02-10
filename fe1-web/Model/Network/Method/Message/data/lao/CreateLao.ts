import { Verifiable } from "Model/Network/Verifiable";
import { Hash, PublicKey, Timestamp } from "Model/Objects";
import { ActionType, MessageData, ObjectType } from "../messageData";

export class CreateLao implements MessageData, Verifiable {

    public readonly object: ObjectType = ObjectType.LAO;
    public readonly action: ActionType = ActionType.CREATE;

    public readonly id: Hash;
    public readonly name: string;
    public readonly creation: Timestamp;
    public readonly organizer: PublicKey;
    public readonly witnesses: PublicKey[];

    constructor(msg: Partial<CreateLao>) {
        Object.assign(this, msg);
    }

    public verify(): boolean {
        // to be implemented...
        return true;
    }
}
