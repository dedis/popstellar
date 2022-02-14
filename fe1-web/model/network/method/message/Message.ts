import {
  Base64UrlData,
  Channel,
  Hash,
  PublicKey,
  Signature,
  WitnessSignature,
  WitnessSignatureState,
} from 'model/objects';
import { KeyPairStore } from 'store';
import { ProtocolError } from 'model/network/ProtocolError';
import { getCurrentPopTokenFromStore } from 'model/objects/wallet/Token';
import {
  MessageData, MessageRegistry, SignatureType,
} from './data';

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

  channel?: Channel;
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
      throw new ProtocolError("Undefined 'signature' parameter encountered during 'Message' creation");
    }

    if (!msg.signature.verify(msg.sender, msg.data) && !this.isElectionResultMessage()) {
      throw new ProtocolError("Invalid 'signature' parameter encountered during 'Message' creation");
    }
    if (!msg.message_id) {
      throw new ProtocolError("Undefined 'message_id' parameter encountered during 'Message' creation");
    }
    const expectedHash = Hash.fromStringArray(msg.data.toString(), msg.signature.toString());
    if (!expectedHash.equals(msg.message_id) && !this.isElectionResultMessage()) {
      console.log('Expected Hash was: ', expectedHash);

      throw new ProtocolError(`Invalid 'message_id' parameter encountered during 'Message' creation: unexpected id value \n
      received message_id was: ${msg.message_id}\n
      Expected message_id is: ${expectedHash}`);
    }
    if (!msg.witness_signatures) {
      throw new ProtocolError("Undefined 'witness_signatures' parameter encountered during 'Message' creation");
    }
    msg.witness_signatures.forEach((ws: WitnessSignature) => {
      if (msg.data === undefined || !ws.verify(msg.data)) {
        throw new ProtocolError(`Invalid 'witness_signatures' parameter encountered: invalid signature from ${ws.signature}`);
      }
    });

    this.sender = msg.sender;
    this.signature = msg.signature;
    this.message_id = msg.message_id;
    this.witness_signatures = [...msg.witness_signatures];

    const jsonData = msg.data.decode();
    const dataObj = JSON.parse(jsonData);
    this.#messageData = messageRegistry.buildMessageData(dataObj as MessageData);
  }

  public static fromJson(obj: any): Message {
    return new Message({
      data: new Base64UrlData(obj.data.toString()),
      sender: new PublicKey(obj.sender.toString()),
      signature: new Signature(obj.signature.toString()),
      message_id: new Hash(obj.message_id.toString()),
      witness_signatures: obj.witness_signatures.map(
        (ws: WitnessSignatureState) => WitnessSignature.fromJson(ws),
      ),
    });
  }

  /**
   * Creates a Message object from a given MessageData and signatures.
   * We don't add the channel property here as we don't want to send that over the network.
   * It signs the messages with the key pair of the user, or the pop token's key pair
   * according to the type of message.
   *
   * @param data - The MessageData to be signed and hashed
   * @param witnessSignatures- The signatures of the witnesses
   * @returns - The created message
   */
  public static async fromData(
    data: MessageData, witnessSignatures?: WitnessSignature[],
  ): Promise<Message> {
    const encodedDataJson: Base64UrlData = encodeMessageData(data);
    let publicKey = KeyPairStore.getPublicKey();
    let privateKey = KeyPairStore.getPrivateKey();
    let signature: Signature;

    // Get the signature type of the type of message we want to sign
    const signatureType = messageRegistry.getSignatureType(data);

    // If the message is signed with the pop token, get it from the store and sign the message
    if (signatureType === SignatureType.POP_TOKEN) {
      const token = await getCurrentPopTokenFromStore();
      if (token) {
        publicKey = token.publicKey;
        privateKey = token.privateKey;
      } else {
        console.error('Impossible to sign the message with a pop token: no token found for '
          + 'current user in this LAO');
      }
      signature = privateKey.sign(encodedDataJson);

      return new Message({
        data: encodedDataJson,
        sender: publicKey,
        signature,
        message_id: Hash.fromStringArray(encodedDataJson.toString(), signature.toString()),
        witness_signatures: (witnessSignatures === undefined) ? [] : witnessSignatures,
      });
    }
    signature = privateKey.sign(encodedDataJson);

    // Otherwise, simply sign with the general key pair
    return new Message({
      data: encodedDataJson,
      sender: publicKey,
      signature,
      message_id: Hash.fromStringArray(encodedDataJson.toString(), signature.toString()),
      witness_signatures: (witnessSignatures === undefined) ? [] : witnessSignatures,
    });
  }

  // This function disables the checks of signature and messageID for eleciton result messages
  // Because the message comes from the back-end and it can't sign the messages since it hasn't
  // access to the private key
  // This method is only a temporary solution for the demo and should be removed once a better
  // solution is found
  private isElectionResultMessage(): boolean {
    return this.data.decode().includes('"result":');
  }
}
