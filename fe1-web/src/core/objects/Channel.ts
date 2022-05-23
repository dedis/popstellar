import { Hash } from './Hash';
import { PublicKey } from './PublicKey';

export type Channel = string;
export const ROOT_CHANNEL: Channel = '/root';

export function getLaoIdFromChannel(ch: Channel): Hash {
  if (!ch || !ch.startsWith(ROOT_CHANNEL)) {
    throw new Error(`Invalid channel path: '${ch}'`);
  }

  const pathComponents = ch.split('/');
  if (pathComponents.length < 3) {
    throw new Error(`LAO is absent from channel path '${ch}'`);
  }

  return new Hash(pathComponents[2]);
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
 * Returns the coin channel
 *
 * @param laoIdHash - The hash containing the laoID of the currently opened LAO
 */
export function getCoinChannel(laoIdHash: Hash): Channel {
  return `${ROOT_CHANNEL}/${laoIdHash.valueOf()}/coin`;
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
 *
 * @remarks
 * Using this function is equivalent to making a lot of assumptions about the channel.
 * This can be brittle and using a more validation-heavy logic may be preferable.
 */
export function getLastPartOfChannel(channel: Channel): Hash {
  const channels = channel.split('/');
  return new Hash(channels[channels.length - 1]);
}
