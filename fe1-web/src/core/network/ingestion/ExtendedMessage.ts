import {
  Base64UrlData,
  Channel,
  getLaoIdFromChannel,
  Hash,
  PublicKey,
  Signature,
  Timestamp,
  WitnessSignature,
  WitnessSignatureState,
} from 'core/objects';

import { Message, ProcessableMessage } from '../jsonrpc/messages';

export interface ExtendedMessageState {
  receivedAt: number;
  receivedFrom: string;
  processedAt?: number;
  channel: Channel;
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

  public readonly receivedFrom: string;

  public readonly processedAt?: Timestamp;

  public readonly channel: Channel;

  get laoId(): Hash | undefined {
    try {
      return getLaoIdFromChannel(this.channel);
    } catch (e) {
      return undefined;
    }
  }

  constructor(msg: Partial<ExtendedMessage>) {
    if (!msg.channel) {
      throw new Error(
        "Undefined 'channel' parameter encountered during 'ExtendedMessage' creation",
      );
    }

    super(msg, msg.channel);

    if (!msg.receivedFrom) {
      throw new Error(
        "Undefined 'receivedFrom' parameter encountered during 'ExtendedMessage' creation",
      );
    }
    this.receivedFrom = msg.receivedFrom;

    this.channel = msg.channel;

    this.receivedAt = msg.receivedAt || Timestamp.EpochNow();
    this.processedAt = msg.processedAt;
  }

  public static fromMessage(
    msg: Message,
    receivedFrom: string,
    channel: Channel,
    receivedAt?: Timestamp,
  ): ExtendedMessage {
    return new ExtendedMessage({
      ...msg,
      channel,
      receivedFrom,
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
      receivedFrom: state.receivedFrom,
      processedAt: state.processedAt ? new Timestamp(state.processedAt) : undefined,
      channel: state.channel,
    });
  }

  public toState(): ExtendedMessageState {
    return JSON.parse(JSON.stringify(this));
  }
}
