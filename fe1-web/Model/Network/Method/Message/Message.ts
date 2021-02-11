import { Base64Data, Hash, PublicKey, KeyPair, Signature, WitnessSignature } from "Model/Objects";
import { MessageData, checkWitnessSignatures, buildMessageData, encodeMessageData } from './data';
import { ProtocolError } from "../../ProtocolError";

export class Message {
  public readonly data: Base64Data;
  public readonly sender: PublicKey;
  public readonly signature: Signature;
  public readonly message_id: Hash;
  public readonly witness_signatures: WitnessSignature[];

  public readonly messageData: MessageData;

  constructor(msg: Partial<Message>) {
    Object.assign(this, msg);

    if (!msg.data)
      throw new ProtocolError('Undefined \'data\' parameter encountered during \'Message\' creation');
    this.data = msg.data;

    let jsonData = msg.data.decode();
    let dataObj = JSON.parse(jsonData);
    this.messageData = buildMessageData(dataObj as MessageData);

    if (!msg.sender)
      throw new ProtocolError('Undefined \'sender\' parameter encountered during \'Message\' creation');
    this.sender = msg.sender;

    if (!msg.signature)
      throw new ProtocolError('Undefined \'signature\' parameter encountered during \'Message\' creation');
    if (!msg.signature.verify(msg.sender, msg.data))
      throw new ProtocolError('Invalid \'signature\' parameter encountered during \'Message\' creation: unexpected message_id value');
    this.signature = msg.signature;

    if (!msg.message_id)
      throw new ProtocolError('Undefined \'message_id\' parameter encountered during \'Message\' creation');
    const expectedHash = Hash.fromStringArray(msg.data.toString(), msg.signature.toString());
    if (expectedHash !== msg.message_id)
      throw new ProtocolError('Invalid \'message_id\' parameter encountered during \'CreateLao\': unexpected id value');
    this.message_id = msg.message_id;

    if (!msg.witness_signatures)
      throw new ProtocolError('Undefined \'witness_signatures\' parameter encountered during \'Message\' creation');
    checkWitnessSignatures(msg.witness_signatures, msg.data);
    this.witness_signatures = [...msg.witness_signatures];
  }

  public static fromData(data: MessageData, witnessSignatures?: WitnessSignature[]): Message {
    const encodedDataJson: Base64Data = encodeMessageData(data);
    const signature: Signature = KeyPair.privateKey.sign(encodedDataJson);

    return new Message({
      data: encodedDataJson,
      sender: KeyPair.publicKey,
      signature: signature,
      message_id: Hash.fromStringArray(encodedDataJson.toString(), signature.toString()),
      witness_signatures: (witnessSignatures === undefined) ? [] : witnessSignatures,
    });
  }
}
