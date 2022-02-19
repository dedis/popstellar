import { Hash } from 'core/objects/Hash';
import { PublicKey } from 'core/objects/PublicKey';

export type Channel = string;
export const ROOT_CHANNEL: Channel = '/root';

export function getLaoIdFromChannel(ch: Channel): Hash {
  // FIXME: implement this
  return new Hash(ch);
}

export function channelFromIds(...args: Hash[]): Channel {
  if (args.length === 0) {
    return ROOT_CHANNEL;
  }

  return `${ROOT_CHANNEL}/${args.map((c) => c.valueOf()).join('/')}`;
}

/**
 * Returns the social channel of the given user.
 *
 * @param laoIdHash - The hash containing the laoID of the currently opened LAO
 * @param userPublicKey - The public key of the user
 */
export function getUserSocialChannel(laoIdHash: Hash, userPublicKey: PublicKey): Channel {
  return `${ROOT_CHANNEL}/${laoIdHash.valueOf()}/social/${userPublicKey}`;
}

/**
 * Returns the general channel of chirps.
 *
 * @param laoIdHash - The hash containing the laoID of the currently opened LAO
 */
export function getGeneralChirpsChannel(laoIdHash: Hash): Channel {
  return `${ROOT_CHANNEL}/${laoIdHash.valueOf()}/social/chirps`;
}

/**
 * Returns the channel of all the reactions.
 *
 * @param laoIdHash - The hash containing the laoID of the currently opened LAO
 */
export function getReactionChannel(laoIdHash: Hash): Channel {
  return `${ROOT_CHANNEL}/${laoIdHash.valueOf()}/social/reactions`;
}

/** Returns the last part of the channel which is usually an event id
 * Example:
 * Input: /root/laoID/electionID
 * Output: electionID
 *
 * @param channel - The channel whose last component we want to obtain
 */
export function getLastPartOfChannel(channel: Channel): Hash {
  const channels = channel.split('/');
  return new Hash(channels[channels.length - 1]);
}
