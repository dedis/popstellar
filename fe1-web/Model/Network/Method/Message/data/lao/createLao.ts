import { Verifiable } from "Model/Network/verifiable";
import { Hash } from "Model/Objects/hash";
import { PublicKey } from "Model/Objects/publicKey";
import { Timestamp } from "Model/Objects/timestamp";
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
}