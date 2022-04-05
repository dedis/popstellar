import { KeyPairStore } from 'core/keypair';
import { ExtendedMessage } from 'core/network/ingestion/ExtendedMessage';
import {
  ActionType,
  AfterProcessingHandler,
  ObjectType,
  ProcessableMessage,
} from 'core/network/jsonrpc/messages';
import { Timestamp } from 'core/objects';
import { dispatch, getStore } from 'core/redux';

import {
  MESSAGE_TO_WITNESS_NOTIFICATION_TYPE,
  WitnessConfiguration,
  WitnessFeature,
} from '../interface';
import { addMessageToWitness, getMessageToWitness } from '../reducer';
import { WitnessMessage } from './messages';
import { WitnessingType, getWitnessRegistryEntry } from './messages/WitnessRegistry';
import { handleWitnessMessage } from './WitnessHandler';
import { requestWitnessMessage } from './WitnessMessageApi';

/**
 * Is executed after a message has been successfully handled.
 * It handles the passive witnessing for messages and prepares
 * the application store for the act of manually witnessing
 * other messages
 */
const afterMessageProcessingHandler =
  (
    addNotification: WitnessConfiguration['addNotification'] /* isLaoWitness: WitnessConfiguration['isLaoWitness'] */,
  ): AfterProcessingHandler =>
  (msg: ProcessableMessage) => {
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

    if (entry) {
      // we have a wintessing entry for this message type

      switch (entry.type) {
        case WitnessingType.PASSIVE:
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

/**
 * Configures the network callbacks in a MessageRegistry.
 *
 * @param config - The witness feature configuration object
 */
export const configureNetwork = (config: WitnessConfiguration) => {
  config.messageRegistry.add(
    ObjectType.MESSAGE,
    ActionType.WITNESS,
    handleWitnessMessage(config.getCurrentLao),
    WitnessMessage.fromJson,
  );

  config.messageRegistry.addAfterProcessingHandler(
    afterMessageProcessingHandler(config.addNotification /* config.isLaoWitness */),
  );
};
