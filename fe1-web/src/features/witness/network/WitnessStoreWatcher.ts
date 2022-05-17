import { Store } from 'redux';

import { KeyPairStore } from 'core/keypair';
import { getMessagesState } from 'core/network/ingestion';
import { ExtendedMessage } from 'core/network/ingestion/ExtendedMessage';
import { Timestamp, Hash } from 'core/objects';
import { dispatch, getStore } from 'core/redux';

import { WitnessConfiguration, WitnessFeature } from '../interface';
import { addMessageToWitness, isMessageToWitness } from '../reducer';
import { getWitnessRegistryEntryType, WitnessingType } from './messages/WitnessRegistry';
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

    const storedMessage = isMessageToWitness(msg.message_id.valueOf(), getStore().getState());
    if (storedMessage) {
      // this message is already stored in the witness reducer
      // and hence does not have to be stored a second time
      return;
    }

    const type = getWitnessRegistryEntryType(msg.messageData);
    const lao = getCurrentLao();

    if (msg.laoId.valueOf() !== lao.id.valueOf()) {
      console.warn(
        `Received a message that should be witnessed for lao ${
          msg.laoId
        } but the current lao is ${lao.id.valueOf()}`,
      );
      return;
    }

    if (type) {
      // we have a witnessing entry for this message type

      switch (type) {
        case WitnessingType.PASSIVE:
          if (!enabled) {
            return;
          }

          requestWitnessMessage(msg.channel, msg.message_id).catch((e) => {
            console.error(
              `Could not witness message with id ${msg.message_id} on channel ${msg.channel}. Error: `,
              e,
              'Message',
              msg,
            );
          });
          break;

        case WitnessingType.ACTIVE:
          // FIXME: only send witness messages if we are a witness
          /* if (!isLaoWitness()) {
           break;
         } */

          dispatch(addMessageToWitness({ messageId: msg.message_id.valueOf() }));
          dispatch(
            addNotification({
              laoId: msg.laoId.valueOf(),
              title: `Witnessing required: ${msg.messageData.object}#${msg.messageData.action}`,
              timestamp: Timestamp.EpochNow().valueOf(),
              type: WitnessFeature.NotificationTypes.MESSAGE_TO_WITNESS,
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
  getCurrentLaoId: () => Hash | undefined,
  afterProcessingHandler: (msg: ExtendedMessage) => void,
) => {
  let previousAllIds: string[] = [];
  let previousUnprocessedIds: string[] = [];

  let currentAllIds: string[] = [];
  let currentUnprocessedIds: string[] = [];
  return () => {
    // we have to be careful with ExtendedMessage.fromState
    // since some message constructors assume that we are connected to a lao
    // thus we delay this watcher until we are connected to a lao
    // (sending witness messages would also be difficult under these circumstances)
    if (!getCurrentLaoId()) {
      return;
    }

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
