import { Store } from 'redux';

import { KeyPairStore } from 'core/keypair';
import { getMessagesState } from 'core/network/ingestion';
import { ExtendedMessage } from 'core/network/ingestion/ExtendedMessage';
import { dispatch, getStore } from 'core/redux';

import {
  MESSAGE_TO_WITNESS_NOTIFICATION_TYPE,
  WitnessConfiguration,
  WitnessFeature,
} from '../interface';
import { addMessageToWitness, getMessageToWitness } from '../reducer';
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
    addNotification: WitnessConfiguration['addNotification'],
    getCurrentLao: WitnessConfiguration['getCurrentLao'],
    /* isLaoWitness: WitnessConfiguration['isLaoWitness'] */
  ) =>
  (msg: ExtendedMessage) => {
    // check if this message has already been signed by us
    const publicKey = KeyPairStore.getPublicKey();
    const signedByUs = msg.witness_signatures.find((sig) =>
      sig.signature.verify(publicKey, msg.message_id),
    );

    if (signedByUs) {
      // if it was, return. we do not have to sign it twice.
      return;
    }

    const storedMessage = getMessageToWitness(msg.message_id.valueOf(), getStore().getState());
    if (storedMessage) {
      // this message is already stored in the witness reducer
      // and hence does not have to be stored a second time
      return;
    }

    const entry = getWitnessRegistryEntry(msg.messageData);
    const lao = getCurrentLao();

    if (entry) {
      // we have a wintessing entry for this message type

      switch (entry.type) {
        case WitnessingType.PASSIVE:
          if (!enabled) {
            return;
          }

          requestWitnessMessage(msg.channel, msg.message_id);
          break;

        case WitnessingType.ACTIVE:
          // only send witness messages if we are a witness
          /* if (!isLaoWitness()) {
           break;
         } */

          dispatch(addMessageToWitness(new ExtendedMessage(msg).toState()));
          dispatch(
            addNotification({
              laoId: lao.id.valueOf(),
              title: `Witnessing required: ${msg.messageData.object}#${msg.messageData.action}`,
              timestamp: Timestamp.EpochNow().valueOf(),
              type: MESSAGE_TO_WITNESS_NOTIFICATION_TYPE,
              messageId: msg.message_id.valueOf(),
            } as WitnessFeature.MessageToWitnessNotification),
          );
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
