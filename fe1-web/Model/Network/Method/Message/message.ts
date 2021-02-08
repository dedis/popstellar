import { MessageData } from './data/messageData';
import { buildMessageData } from './data/builder';
import { WitnessSignature } from '../../../Objects/witnessSignature';
import { PublicKey } from '../../../Objects/publicKey';
import { Hash } from '../../../Objects/hash';
import { Signature } from '../../../Objects/signature';
import { Base64Data } from '../../../Objects/base64';

export class Message {
    public readonly data : Base64Data;
    public readonly sender : PublicKey;
    public readonly signature : Signature;
    public readonly message_id : Hash;
    public readonly witness_signatures : WitnessSignature[];

    private _messageData : MessageData;

    constructor(message: Partial<Message>) {
        Object.assign(this, message);

        let decoded : unknown = this.data.decode();
        this._messageData = buildMessageData(decoded as MessageData);
    }

    public verify(): boolean {
        let res = true;

        // verify sender's signature on data

        // verify witnesses' signature
        this.witness_signatures.forEach(witSig => {
            res = res && witSig.verify(this.message_id);
        });

        return res;
    }
}