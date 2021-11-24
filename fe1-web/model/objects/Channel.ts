import { KeyPairStore, OpenedLaoStore } from 'store';
import { Hash } from './Hash';
import { getCurrentPopToken } from './wallet';

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
export function getCurrentUserSocialChannel(laoIdHash: Hash): Channel | undefined {
  const currentLao = OpenedLaoStore.get();

  const isOrganizer = (KeyPairStore.getPublicKey() === currentLao.organizer);
  let userPublicKey = '';

  // If the current user is an organizer, simply get his public key from KeyPairStore
  if (isOrganizer) {
    userPublicKey = KeyPairStore.getPublicKey().valueOf();
  } else {
    // If the current user is an attendee, we need to get his pop token
    getCurrentPopToken(laoIdHash).catch((err) => {
      console.error('Could not get pop token of user to send a chirp, error: ', err);
    }).then((token) => {
      if (token) {
        userPublicKey = token.publicKey.valueOf();
      } else {
        console.error('Sending a chirp is impossible: no token found for current user');
      }
    });
  }

  if (userPublicKey === '') {
    return undefined;
  }
  return `${ROOT_CHANNEL}/${laoIdHash.valueOf()}/social/${userPublicKey.valueOf()}`;
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
