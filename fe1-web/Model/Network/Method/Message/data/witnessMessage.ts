import { Hash } from "../../../../Objects/hash";
import { Signature } from "../../../../Objects/signature";
import { ActionType, MessageData, ObjectType } from "./messageData";

export class WitnessMessage implements MessageData {

    public readonly object: ObjectType;
    public readonly action: ActionType;

    public readonly message_id: Hash;
    public readonly signature: Signature;

    constructor(msg: Partial<WitnessMessage>) {
        Object.assign(this, msg);
    }
}
