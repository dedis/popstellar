import {
  Base64UrlData,
  Base64UrlDataState,
  Channel,
  getLaoIdFromChannel,
  Hash,
  HashState,
  ProtocolError,
  PublicKey,
  PublicKeyState,
  Signature,
  SignatureState,
  Timestamp,
  TimestampState,
  WitnessSignature,
  WitnessSignatureState,
} from 'core/objects';

import { Message, ProcessableMessage } from '../jsonrpc/messages';

export interface ExtendedMessageState {
  receivedAt: TimestampState;
  receivedFrom: string;
  processedAt?: TimestampState;
  channel: Channel;
  laoId: HashState;

  data: Base64UrlDataState;
  sender: PublicKeyState;
  signature: SignatureState;
  message_id: HashState;
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
    if (!this.laoId) {
      throw new ProtocolError(`Cannot call .toState() on ExtendesMessage with undefined laoId`);
    }

    return {
      receivedAt: this.receivedAt.toState(),
      receivedFrom: this.receivedFrom,
      processedAt: this.processedAt?.toState(),
      channel: this.channel,
      laoId: this.laoId?.toState(),

      data: this.data.toState(),
      sender: this.sender.toState(),
      signature: this.signature.toState(),
      message_id: this.message_id.toState(),
      witness_signatures: this.witness_signatures.map((sig) => sig.toState()),
    };
  }
}
