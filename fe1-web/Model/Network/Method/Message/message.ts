import { Base64Data, Hash, PublicKey, KeyPair, Signature, WitnessSignature } from "Model/Objects";
import { Verifiable } from 'Model/Network/Verifiable';
import { ProtocolError } from 'Model/Network/ProtocolError';
import { MessageData } from './data/messageData';
import { buildMessageData } from './data/builder';

export class Message implements Verifiable {
    public readonly data : Base64Data;
    public readonly sender : PublicKey;
    public readonly signature : Signature;
    public readonly message_id : Hash;
    public readonly witness_signatures : WitnessSignature[];

    public readonly messageData: MessageData;

    constructor(message: Partial<Message>) {
        Object.assign(this, message);

        let jsonData = this.data.decode();
        let dataObj = JSON.parse(jsonData);
        this.messageData = buildMessageData(dataObj as MessageData);
    }

    public static fromData(data: {}, witnessSignatures?: WitnessSignature[]): Message {
        const encodedDataJson: Base64Data = Base64Data.encode(JSON.stringify(buildMessageData(data as MessageData)));
        const signature: Signature = KeyPair.privateKey.sign(encodedDataJson);

        return new Message({
            data: encodedDataJson,
            sender: KeyPair.publicKey,
            signature: signature,
            message_id: Hash.fromString(encodedDataJson.toString(), signature.toString()),
            witness_signatures: (witnessSignatures === undefined) ? [] : witnessSignatures,
        });
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
