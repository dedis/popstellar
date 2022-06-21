import { Dispatch } from 'redux';

import { getNetworkManager, subscribeToChannel } from 'core/network';
import { NetworkConnection } from 'core/network/NetworkConnection';

import { Lao, LaoState } from '../objects/Lao';

/**
 * Connects to a known lao
 * @param lao The lao to connect to
 * @returns The list of new network connections
 */
export const connectToLao = (lao: Lao): NetworkConnection[] =>
  lao.server_addresses.map((address) => getNetworkManager().connect(address));

/**
 * Resubscribes to a known lao
 * @param lao The lao that should be re-subscribed to
 * @param dispatch A redux store dispatch function
 * @param connections An optional list of connections on which the subscribe messages should be sent
 */
export const resubscribeToLao = async (
  lao: Lao | LaoState,
  dispatch: Dispatch,
  connections?: NetworkConnection[],
) => {
  await Promise.all(
    lao.subscribed_channels.map((channel) =>
      subscribeToChannel(lao.id, dispatch, channel, connections),
    ),
  );
};
