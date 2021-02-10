import { Hash } from './hash';

export type Channel = string;
export const ROOT_CHANNEL: Channel = '/root';

export function channelFromId(value?: Hash) : Channel {
    if ( value === undefined ){
        return ROOT_CHANNEL;
    } else {
        let ch = value.valueOf();
        return `${ROOT_CHANNEL}/${ch}`;
    }
}