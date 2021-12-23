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
 * Returns the social channel of the given user.
 *
 * @param laoIdHash - The hash containing the laoID of the currently opened LAO
 * @param userToken - The pop token of the user
 */
export function getUserSocialChannel(laoIdHash: Hash, userToken: string): Channel {
  return `${ROOT_CHANNEL}/${laoIdHash.valueOf()}/social/${userToken}`;
}

/**
 * Returns the general channel of chirps.
 *
 * @param laoIdHash - The hash containing the laoID of the currently opened LAO
 */
export function getGeneralChirpsChannel(laoIdHash: Hash): Channel {
  return `${ROOT_CHANNEL}/${laoIdHash.valueOf()}/social/chirps`;
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
