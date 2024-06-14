import { getSigningKeyPair, publish } from 'core/network';
import { Message } from 'core/network/jsonrpc/messages';
import { getFederationChannel, Hash, PublicKey, Timestamp } from 'core/objects';

import { Challenge } from '../objects/Challenge';
import { ChallengeRequest, ChallengeMessage, FederationExpect, FederationInit } from './messages';

/**
 * Contains all functions to send social media related messages.
 */

/**
 * Sends a query to the server to request a new challenge.
 *
 */
export function requestChallenge(laoId: Hash): Promise<void> {
  const timestamp = Timestamp.EpochNow();
  const message = new ChallengeRequest({
    timestamp: timestamp,
  });
  return publish(getFederationChannel(laoId), message);
}

/**
 * Sends a query to the server to init a new federation
 *
 */
export async function initFederation(
  lao_id: Hash,
  linked_lao_id: Hash,
  server_address: String,
  public_key: PublicKey,
  challenge: Challenge,
): Promise<void> {
  const msgData = new ChallengeMessage({
    value: challenge.value,
    valid_until: challenge.valid_until,
  });
  const keyPair = await getSigningKeyPair(msgData);
  const channel = getFederationChannel(lao_id);
  const messageChallenge = Message.fromData(msgData, keyPair, channel);
  const message = new FederationInit({
    lao_id: linked_lao_id,
    server_address: server_address,
    public_key: public_key,
    challenge: messageChallenge,
  });
  return publish(channel, message);
}

/**
 * Sends a query to the server to expect a new federation
 *
 */
export async function expectFederation(
  lao_id: Hash,
  linked_lao_id: Hash,
  server_address: String,
  public_key: PublicKey,
  challenge: Challenge,
): Promise<void> {
  const msgData = new ChallengeMessage({
    value: challenge.value,
    valid_until: challenge.valid_until,
  });
  const keyPair = await getSigningKeyPair(msgData);
  const channel = getFederationChannel(lao_id);
  const messageChallenge = Message.fromData(msgData, keyPair, channel);
  const message = new FederationExpect({
    lao_id: linked_lao_id,
    server_address: server_address,
    public_key: public_key,
    challenge: messageChallenge,
  });
  return publish(channel, message);
}
