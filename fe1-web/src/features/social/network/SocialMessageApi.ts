import { publish } from 'core/network';
import { getReactionChannel, getUserSocialChannel, Hash, PublicKey, Timestamp } from 'core/objects';

import { AddChirp, DeleteChirp } from './messages/chirp';
import { AddReaction, DeleteReaction } from './messages/reaction';

/**
 * Contains all functions to send social media related messages.
 */

/**
 * Sends a query to the server to add a new chirp.
 *
 * @param publicKey - The public key of the sender
 * @param text - The text contained in the chirp
 * @param laoId - The id of the Lao in which to send the chirp
 * @param parentId - The id of the parent chirp (if it is a reply)
 */
export function requestAddChirp(
  publicKey: PublicKey,
  text: string,
  laoId: Hash,
  parentId?: Hash,
): Promise<void> {
  const timestamp = Timestamp.EpochNow();

  const message = new AddChirp({
    text: text,
    parent_id: parentId,
    timestamp: timestamp,
  });

  return publish(getUserSocialChannel(laoId, publicKey), message);
}

/**
 * Sends a query to the server to delete a chirp.
 *
 * @param publicKey - The public key of the sender
 * @param chirpId - The id of the chirp to be deleted
 * @param laoId - The id of the Lao in which to delete a chirp
 */
export function requestDeleteChirp(
  publicKey: PublicKey,
  chirpId: Hash,
  laoId: Hash,
): Promise<void> {
  const timestamp = Timestamp.EpochNow();

  const message = new DeleteChirp({
    chirp_id: chirpId,
    timestamp: timestamp,
  });

  return publish(getUserSocialChannel(laoId, publicKey), message);
}

/**
 * Sends a query to the server to add a new reaction.
 *
 * @param reaction_codepoint - The codepoint corresponding to the reaction type
 * @param chirp_id - The id of the chirp where the reaction is added
 * @param laoId - The id of the Lao in which to add a reaction
 */
export function requestAddReaction(
  reaction_codepoint: string,
  chirp_id: Hash,
  laoId: Hash,
): Promise<void> {
  const timestamp = Timestamp.EpochNow();

  const message = new AddReaction({
    reaction_codepoint: reaction_codepoint,
    chirp_id: chirp_id,
    timestamp: timestamp,
  });

  return publish(getReactionChannel(laoId), message);
}

/**
 * Sends a query to the server to add a new reaction.
 *
 * @param reaction_codepoint - The codepoint corresponding to the reaction type
 * @param chirp_id - The id of the chirp where the reaction is added
 * @param laoId - The id of the Lao in which to add a reaction
 */
export function requestDeleteReaction(reactionId: Hash, laoId: Hash): Promise<void> {
  const message = new DeleteReaction({
    reaction_id: reactionId,
    timestamp: Timestamp.EpochNow(),
  });

  return publish(getReactionChannel(laoId), message);
}
