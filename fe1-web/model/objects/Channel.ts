import { Hash } from './Hash';
import { KeyPairStore } from '../../store';

export type Channel = string;
export const ROOT_CHANNEL: Channel = '/root';

export function channelFromIds(...args: Hash[]) : Channel {
  if (args.length === 0) return ROOT_CHANNEL;

  return `${ROOT_CHANNEL}/${
    args.map((c) => c.valueOf())
      .join('/')}`;
}

export function userSocialChannel(laoId: Hash): Channel {
  return `${ROOT_CHANNEL}/${laoId.valueOf()}/social/${KeyPairStore.get().publicKey.valueOf()}`;
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
