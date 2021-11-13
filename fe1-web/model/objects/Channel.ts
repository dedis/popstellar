import { KeyPairStore } from 'store';
import { Hash } from './Hash';

export type Channel = string;
export const ROOT_CHANNEL: Channel = '/root';

export function channelFromIds(...args: Hash[]) : Channel {
  if (args.length === 0) return ROOT_CHANNEL;

  return `${ROOT_CHANNEL}/${
    args.map((c) => c.valueOf())
      .join('/')}`;
}

/**
 * Returns the social channel of the current user.
 *
 * @param laoIdHash - The hash containing the laoID of the currently opened LAO
 */
export function userSocialChannel(laoIdHash: Hash): Channel {
  const userPublicKey = KeyPairStore.getPublicKey().valueOf();
  return `${ROOT_CHANNEL}/${laoIdHash.valueOf()}/social/${userPublicKey}`;
}

/**
 * Returns the general channel of chirps.
 *
 * @param laoIdHash - The hash containing the laoID of the currently opened LAO
 */
export function generalChirpsChannel(laoIdHash: Hash): Channel {
  return `${ROOT_CHANNEL}/${laoIdHash.valueOf()}/social/chirps`;
}

/** Returns the last part of the channel which is usually an event id
 * Example:
 * Input: /root/laoID/electionID
 * Output: electionID
 *
 * @param channel - The channel we want the last port
 */
export function getLastChannel(channel: Channel): Hash {
  const channels = channel.split('/');
  return new Hash(channels[channels.length - 1]);
}
