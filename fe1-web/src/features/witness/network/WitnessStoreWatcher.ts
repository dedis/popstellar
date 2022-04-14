import { Store } from 'redux';

import { getMessagesState } from 'core/network/ingestion';
import { ExtendedMessage } from 'core/network/ingestion/ExtendedMessage';

import { WitnessConfiguration } from '../interface';
import { WitnessingType, getWitnessRegistryEntry } from './messages/WitnessRegistry';
import { requestWitnessMessage } from './WitnessMessageApi';

/**
 * Is executed after a message has been successfully handled.
 * It handles the passive witnessing for messages and prepares
 * the application store for the act of manually witnessing
 * other messages
 */
export const afterMessageProcessingHandler =
  (
    enabled: WitnessConfiguration['enabled'],
    /* isLaoWitness: WitnessConfiguration['isLaoWitness'] */
  ) =>
  (msg: ExtendedMessage) => {
    const entry = getWitnessRegistryEntry(msg.messageData);

    if (entry) {
      // we have a wintessing entry for this message type
      switch (entry.type) {
        case WitnessingType.PASSIVE:
          if (enabled) {
            requestWitnessMessage(msg.channel, msg.message_id);
          }
          break;

        case WitnessingType.ACTIVE:
          break;

        case WitnessingType.NO_WITNESSING:
        default:
          break;
      }
    }
  };

/** Creates a function that reacts to newly processed messages by calling
 * 'afterMessageProcessingHandler'
 * @remarks
 * Follows the implementation of makeMessageStoreWatcher but reacts to newly
 * processed messages instead of newly unprocessed ones Even it is tempting to
 * just check for changes in unprocessedIds, we should not rely on this function
 * being called after every dispatch(). Thus it is possible that a
 * message has skipped unprocessedIds and we have to check allIds as well
 * @param store The redux store
 * @returns The listener that can be passed to store.subscribe()
 */
export const makeWitnessStoreWatcher = (
  store: Store,
  afterProcessingHandler: (msg: ExtendedMessage) => void,
) => {
  let previousAllIds: string[] = [];
  let previousUnprocessedIds: string[] = [];

  let currentAllIds: string[] = [];
  let currentUnprocessedIds: string[] = [];
  return () => {
    const state = store.getState();
    const msgState = getMessagesState(state);
    const allIds = msgState?.allIds || [];
    previousAllIds = currentAllIds;
    currentAllIds = allIds;

    const unprocessedIds = msgState?.unprocessedIds || [];
    previousUnprocessedIds = currentUnprocessedIds;
    currentUnprocessedIds = unprocessedIds;

    if (
      previousAllIds.length === currentAllIds.length &&
      previousAllIds.every((value, index) => value === currentAllIds[index]) &&
      previousUnprocessedIds.length === currentUnprocessedIds.length &&
      previousUnprocessedIds.every((value, index) => value === currentUnprocessedIds[index])
    ) {
      // no change detected
      return;
    }

    // get all message ids that are part of currentAllIds
    for (const msgId of currentAllIds) {
      // and that are currently not part of unprocessedIds
      if (currentUnprocessedIds.includes(msgId)) {
        return;
      }
      // and that either have not been part of previousAllIds OR
      // have been part of previousUnprocessedIds
      if (previousAllIds.includes(msgId) && !previousUnprocessedIds.includes(msgId)) {
        return;
      }

      // i.e. all messages that have been processed
      // since the last call of this function

      afterProcessingHandler(ExtendedMessage.fromState(msgState.byId[msgId]));
    }
  };
};
