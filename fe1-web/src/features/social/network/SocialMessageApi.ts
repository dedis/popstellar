import { publish } from 'core/network';
import { getReactionChannel, getUserSocialChannel, Hash, PublicKey, Timestamp } from 'core/objects';
import { Lao } from 'features/lao/objects'; // TODO: Change this
import { OpenedLaoStore } from 'features/lao/store'; // TODO: Change this
import { AddChirp, DeleteChirp } from './messages/chirp';
import { AddReaction } from './messages/reaction';

/**
 * Contains all functions to send social media related messages.
 */

/**
 * Sends a query to the server to add a new chirp.
 *
 * @param publicKey - The public key of the sender
 * @param text - The text contained in the chirp
 * @param parentId - The id of the parent chirp (if it is a reply)
 */
export function requestAddChirp(
  publicKey: PublicKey,
  text: string,
  parentId?: Hash,
): Promise<void> {
  const timestamp = Timestamp.EpochNow();
  const currentLao: Lao = OpenedLaoStore.get();

  const message = new AddChirp({
    text: text,
    parent_id: parentId,
    timestamp: timestamp,
  });

  return publish(getUserSocialChannel(currentLao.id, publicKey), message);
}

/**
 * Sends a query to the server to delete a chirp.
 *
 * @param publicKey - The public key of the sender
 * @param chirpId - The id of the chirp to be deleted
 */
export function requestDeleteChirp(publicKey: PublicKey, chirpId: Hash): Promise<void> {
  const timestamp = Timestamp.EpochNow();
  const currentLao: Lao = OpenedLaoStore.get();

  const message = new DeleteChirp({
    chirp_id: chirpId,
    timestamp: timestamp,
  });

  return publish(getUserSocialChannel(currentLao.id, publicKey), message);
}

/**
 * Sends a query to the server to add a new reaction.
 *
 * @param reaction_codepoint - The codepoint corresponding to the reaction type
 * @param chirp_id - The id of the chirp where the reaction is added
 */
export function requestAddReaction(reaction_codepoint: string, chirp_id: Hash): Promise<void> {
  const timestamp = Timestamp.EpochNow();
  const currentLao: Lao = OpenedLaoStore.get();

  const message = new AddReaction({
    reaction_codepoint: reaction_codepoint,
    chirp_id: chirp_id,
    timestamp: timestamp,
  });

  return publish(getReactionChannel(currentLao.id), message);
}
