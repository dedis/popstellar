import {
  Base64UrlData,
  Channel,
  Hash,
  PublicKey,
  Signature,
  ProtocolError,
  WitnessSignature,
  WitnessSignatureState,
  KeyPair,
} from 'core/objects';

import { MessageRegistry } from './MessageRegistry';
import { MessageData } from './MessageData';

let messageRegistry: MessageRegistry;

/**
 * Dependency injection of a MessageRegistry to know how messages need to be signed and how they
 * are built.
 *
 * @param registry - The MessageRegistry to be injected
 */
export function configureMessages(registry: MessageRegistry) {
  messageRegistry = registry;
}

/**
 * Encodes a MessageData into a Base64Url.
 *
 * @param msgData - The MessageData to be encoded
 * @returns Base64UrlData - The encoded message
 *
 * @remarks
 * This is exported for testing purposes.
 */
export function encodeMessageData(msgData: MessageData): Base64UrlData {
  const data = JSON.stringify(msgData);
  return Base64UrlData.encode(data);
}

/**
 * MessageState is the interface that should match JSON.stringify(Message)
 * It is used to store messages in a way compatible with the Redux store.
 */
export interface MessageState {
  data: string;

  sender: string;

  signature: string;

  message_id: string;

  witness_signatures: WitnessSignatureState[];

  channel: Channel;
}

/**
 * Message represents the Message object in the PoP protocol
 */
export class Message {
  public readonly data: Base64UrlData;

  public readonly sender: PublicKey;

  public readonly signature: Signature;

  public readonly message_id: Hash;

  public readonly witness_signatures: WitnessSignature[];

  // Not part of the protocol, but convenient for processing of the message
  public readonly channel?: Channel;

  // ECMAScript private field, not string-ified by JSON
  readonly #messageData: MessageData;

  // Public getter that makes messageData appear public
  public get messageData() {
    return this.#messageData;
  }

  constructor(msg: Partial<Message>) {
    if (!msg.data) {
      throw new ProtocolError("Undefined 'data' parameter encountered during 'Message' creation");
    }
    this.data = msg.data;
    if (!msg.sender) {
      throw new ProtocolError("Undefined 'sender' parameter encountered during 'Message' creation");
    }
    if (!msg.signature) {
      throw new ProtocolError(
        "Undefined 'signature' parameter encountered during 'Message' creation",
      );
    }

    if (!msg.signature.verify(msg.sender, msg.data)) {
      throw new ProtocolError(
        "Invalid 'signature' parameter encountered during 'Message' creation",
      );
    }
    if (!msg.message_id) {
      throw new ProtocolError(
        "Undefined 'message_id' parameter encountered during 'Message' creation",
      );
    }
    const expectedHash = Hash.fromStringArray(msg.data.toString(), msg.signature.toString());
    if (!expectedHash.equals(msg.message_id)) {
      console.log('Expected Hash was: ', expectedHash);

      throw new ProtocolError(`Invalid 'message_id' parameter encountered during 'Message' creation: unexpected id value \n
      received message_id was: ${msg.message_id}\n
      Expected message_id is: ${expectedHash}`);
    }
    if (!msg.witness_signatures) {
      throw new ProtocolError(
        "Undefined 'witness_signatures' parameter encountered during 'Message' creation",
      );
    }
    msg.witness_signatures.forEach((ws: WitnessSignature) => {
      if (msg.data === undefined || !ws.verify(msg.data)) {
        throw new ProtocolError(
          `Invalid 'witness_signatures' parameter encountered: invalid signature from ${ws.signature}`,
        );
      }
    });

    this.sender = msg.sender;
    this.signature = msg.signature;
    this.message_id = msg.message_id;
    this.witness_signatures = [...msg.witness_signatures];

    if (msg.channel) {
      this.channel = msg.channel;
    }

    const jsonData = msg.data.decode();
    const dataObj = JSON.parse(jsonData);
    this.#messageData = messageRegistry.buildMessageData(dataObj);
  }

  public static fromJson(obj: any, channel?: Channel): Message {
    return new Message({
      data: new Base64UrlData(obj.data.toString()),
      sender: new PublicKey(obj.sender.toString()),
      signature: new Signature(obj.signature.toString()),
      message_id: new Hash(obj.message_id.toString()),
      witness_signatures: obj.witness_signatures.map((ws: WitnessSignatureState) =>
        WitnessSignature.fromJson(ws),
      ),
      channel: channel,
    });
  }

  /**
   * Creates a Message object from a given MessageData and signatures.
   * We don't add the channel property here as we don't want to send that over the network.
   * It signs the messages with the key pair of the user.
   *
   * @param data - The MessageData to be signed and hashed
   * @param senderKeyPair - The key pair of the sender
   * @param witnessSignatures- The signatures of the witnesses
   * @returns - The created message
   */
  public static async fromData(
    data: MessageData,
    senderKeyPair: KeyPair,
    witnessSignatures?: WitnessSignature[],
  ): Promise<Message> {
    const encodedDataJson: Base64UrlData = encodeMessageData(data);
    const { publicKey, privateKey } = senderKeyPair;
    const signature = privateKey.sign(encodedDataJson);

    return new Message({
      data: encodedDataJson,
      sender: publicKey,
      signature: signature,
      message_id: Hash.fromStringArray(encodedDataJson.toString(), signature.toString()),
      witness_signatures: witnessSignatures === undefined ? [] : witnessSignatures,
    });
  }
}
