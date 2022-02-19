import {
  Hash,
  PublicKey,
  Base64UrlData,
  Signature,
  Timestamp,
  Channel,
  WitnessSignature,
  WitnessSignatureState,
  getLaoIdFromChannel,
} from 'core/objects';

import { Message } from '../jsonrpc/messages/Message';
import { ProcessableMessage } from '../jsonrpc/messages/ProcessableMessage';

export interface ExtendedMessageState {
  receivedAt: number;
  processedAt?: number;
  channel?: Channel;
  laoId: string;

  data: string;
  sender: string;
  signature: string;
  message_id: string;
  witness_signatures: WitnessSignatureState[];
}

export function markMessageAsProcessed(
  msg: ExtendedMessageState,
  when?: Timestamp,
): ExtendedMessageState {
  return {
    ...msg,
    processedAt: when?.valueOf() || Timestamp.EpochNow().valueOf(),
  };
}

export class ExtendedMessage extends Message implements ProcessableMessage {
  public readonly receivedAt: Timestamp;

  public processedAt?: Timestamp;

  // The channel on which the message was received
  public channel: Channel;

  constructor(msg: Partial<ExtendedMessage>) {
    super(msg);
    this.receivedAt = msg.receivedAt || Timestamp.EpochNow();
    this.processedAt = msg.processedAt;
    this.channel =
      msg.channel ||
      (() => {
        throw new Error('channel not defined');
      })();
  }

  get laoId(): Hash {
    return getLaoIdFromChannel(this.channel);
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
