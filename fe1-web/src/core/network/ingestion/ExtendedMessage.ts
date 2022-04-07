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

  public readonly receivedFrom: string;

  public processedAt?: Timestamp;

  // The channel on which the message was received
  public channel: Channel;

  constructor(msg: Partial<ExtendedMessage>) {
    super(msg);

    if (!msg.receivedFrom) {
      throw new Error(
        '"receivedFrom" is not defined when creating a new instance of ExtendedMessage',
      );
    }

    this.receivedAt = msg.receivedAt || Timestamp.EpochNow();
    this.receivedFrom = msg.receivedFrom;
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

  public static fromMessage(
    msg: Message,
    ch: Channel,
    receivedFrom: string,
    receivedAt?: Timestamp,
  ): ExtendedMessage {
    return new ExtendedMessage({
      ...msg,
      channel: ch,
      receivedAt: receivedAt || Timestamp.EpochNow(),
      receivedFrom,
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
