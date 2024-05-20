import { publish } from 'core/network';
import { getFederationChannel, Hash, Timestamp } from 'core/objects';

import { ChallengeRequest } from './messages';

/**
 * Contains all functions to send social media related messages.
 */

/**
 * Sends a query to the server to request a new challenge.
 *
 */
export function requestChallenge(
  laoId: Hash,
): Promise<void> {
  const timestamp = Timestamp.EpochNow();
  const message = new ChallengeRequest({
    timestamp: timestamp,
  });
  return publish(getFederationChannel(laoId), message);
}
