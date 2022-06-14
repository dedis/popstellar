import {
  Base64UrlData,
  Channel,
  Hash,
  PublicKey,
  Signature,
  Timestamp,
  WitnessSignature,
} from 'core/objects';

import { MessageData } from './MessageData';

export interface ProcessableMessage {
  receivedAt: Timestamp;
  processedAt?: Timestamp;
  // the address this message was received from
  receivedFrom: string;

  channel: Channel;
  laoId?: Hash;

  data: Base64UrlData;
  sender: PublicKey;
  signature: Signature;
  message_id: Hash;
  witness_signatures: WitnessSignature[];

  messageData: MessageData;
}
