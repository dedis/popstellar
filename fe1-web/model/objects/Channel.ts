import { KeyPairStore } from 'store';
import { Hash } from './Hash';
import { PublicKey } from './PublicKey';

export type Channel = string;
export const ROOT_CHANNEL: Channel = '/root';

export function channelFromIds(...args: Hash[]) : Channel {
  if (args.length === 0) return ROOT_CHANNEL;

  return `${ROOT_CHANNEL}/${
    args.map((c) => c.valueOf())
      .join('/')}`;
}

/**
 * Returns the chirp channel of the current user.
 *
 * @param laoIdHash - The hash containing the laoID of the currently opened LAO
 */
export function getCurrentUserChirpChannel(laoIdHash: Hash): Channel {
  const userPublicKey = KeyPairStore.get().publicKey.valueOf();
  return `${ROOT_CHANNEL}/${laoIdHash.valueOf()}/social/${userPublicKey}`;
}

/**
 * Returns the chirp channel for a specific user.
 *
 * @param laoIdHash - The hash containing the laoID of the current opened LAO
 * @param userPk - The public key of the user
 */
export function getUserChirpChannel(laoIdHash: Hash, userPk: PublicKey): Channel {
  return `${ROOT_CHANNEL}/${laoIdHash.valueOf()}/social/${userPk.valueOf()}`;
}

/**
 * Returns the general channel of chirps.
 *
 * @param laoIdHash - The hash containing the laoID of the currently opened LAO
 */
export function getGeneralChirpChannel(laoIdHash: Hash): Channel {
  return `${ROOT_CHANNEL}/${laoIdHash.valueOf()}/social/chirps`;
}

/** Returns the last part of the channel which is usually an event id
 * Example:
 * Input: /root/laoid/electionid
 * Output: electionId
 */
export function getLastChannel(channel: Channel): Hash {
  const channels = channel.split('/');
  return new Hash(channels[channels.length - 1]);
}
