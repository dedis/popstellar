import {
  Base64UrlData,
  Channel,
  Hash,
  PublicKey,
  Signature,
  Timestamp,
  WitnessSignature,
  WitnessSignatureState,
} from 'core/objects';
import { MessageData } from './MessageData';

export interface ProcessableMessage {
  receivedAt: Timestamp;
  processedAt?: Timestamp;
  channel?: Channel;

  data: Base64UrlData;
  sender: PublicKey;
  signature: Signature;
  message_id: Hash;
  witness_signatures: WitnessSignature[];

  messageData: MessageData;
}
