import { Hash, PublicKey, Base64UrlData, Signature, Timestamp, Channel, WitnessSignature } from 'core/objects';

import { Message, MessageState } from './Message';

export interface ExtendedMessageState extends MessageState {
  receivedAt: number;
  processedAt?: number;
  channel?: string;
}

export function markExtMessageAsProcessed(
  msg: ExtendedMessageState,
  when?: Timestamp,
): ExtendedMessageState {
  return {
    ...msg,
    processedAt: when?.valueOf() || Timestamp.EpochNow().valueOf(),
  };
}

export class ExtendedMessage extends Message {
  public readonly receivedAt: Timestamp;

  public processedAt?: Timestamp;

  // The channel field gets assigned for all incoming messages in JsonRpcWithMessage.ts
  // In order to use it when handling the messages, such as the election result msg
  public channel?: Channel;

  constructor(msg: Partial<ExtendedMessage>) {
    super(msg);
    this.receivedAt = msg.receivedAt || Timestamp.EpochNow();
    this.processedAt = msg.processedAt;

    if (msg.channel) {
      this.channel = msg.channel;
    }
  }

  public static fromMessage(msg: Message, ch: Channel, receivedAt?: Timestamp): ExtendedMessage {
    return new ExtendedMessage({
      ...msg,
      channel: ch,
      receivedAt: receivedAt || Timestamp.EpochNow(),
    });
  }

  public static fromState(state: ExtendedMessageState): ExtendedMessage {
    return new ExtendedMessage({
      // message fields:
      data: new Base64UrlData(state.data),
      sender: new PublicKey(state.sender),
      signature: new Signature(state.signature),
      message_id: new Hash(state.message_id),
      witness_signatures: state.witness_signatures.map(
        (ws: any) =>
          new WitnessSignature({
            witness: new PublicKey(ws.witness),
            signature: new Signature(ws.signature),
          }),
      ),

      // extended fields:
      receivedAt: state.receivedAt ? new Timestamp(state.receivedAt) : undefined,
      processedAt: state.processedAt ? new Timestamp(state.processedAt) : undefined,
      channel: state.channel,
    });
  }

  public toState(): ExtendedMessageState {
    return JSON.parse(JSON.stringify(this));
  }
}
