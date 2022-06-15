import { Dispatch } from 'redux';

import { Channel, Hash } from 'core/objects';
import { addSubscribedChannel, removeSubscribedChannel } from 'features/lao/reducer';

import { storeMessage } from './ingestion';
import { catchup, subscribe, unsubscribe } from './JsonRpcApi';
import { NetworkConnection } from './NetworkConnection';

/**
 * Subscribes to a channel and optionally catches up to previously sent messages
 * @remark Stores the name of the channel in the store as a workaround for https://github.com/dedis/popstellar/issues/1078
 *
 * @param laoId The id of the lao
 * @param dispatch The dispatch function of the redux store
 * @param channel The channel to subscribe to
 * @param connections An optional list of network connection if the message should only be sent on a subset of connections
 * @param sendCatchup Whether to send a catchup message after subscribing to the channel. Default value is 'true'
 * @returns A promise to wait on the subscription (and the optional catchup)
 */
export async function subscribeToChannel(
  laoId: Hash | string,
  dispatch: Dispatch,
  channel: Channel,
  connections?: NetworkConnection[],
  sendCatchup: boolean = true,
) {
  if (!channel) {
    throw new Error('Could not subscribe to channel without a valid channel');
  }

  console.debug('Subscribing to channel: ', channel);
  dispatch(addSubscribedChannel(laoId, channel));

  try {
    // Subscribe to the channel
    await subscribe(channel, connections);

    if (sendCatchup) {
      // Retrieve all previous LAO messages in the form of a generator
      const msgs = await catchup(channel, connections);

      for (const msg of msgs) {
        storeMessage(msg);
      }
    }

    return;
  } catch (err) {
    console.error('Something went wrong when subscribing to channel', err);
    throw err;
  }
}

/**
 * Unsubscribes from a channel
 * @param laoId The id of the lao
 * @param dispatch The dispatch function of the redux store
 * @param channel The channel to unsubscribe from
 * @param connections An optional list of network connection if the message should only be sent on a subset of connections
 * @returns A promise to wait on the unsubscription
 */
export async function unsubscribeFromChannel(
  laoId: Hash | string,
  dispatch: Dispatch,
  channel: Channel,
  connections?: NetworkConnection[],
) {
  if (!channel) {
    throw new Error('Could not unsubscribe from channel without a valid channel');
  }

  console.debug('Unsubscribing from channel: ', channel);
  dispatch(removeSubscribedChannel(laoId, channel));

  try {
    // Unsubscribe from the channel
    await unsubscribe(channel, connections);

    return;
  } catch (err) {
    console.error('Something went wrong when subscribing to channel', err);
    throw err;
  }
}
