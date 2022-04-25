import { Store } from 'redux';

import { getNetworkManager } from 'core/network';
import { getMessagesState } from 'core/network/ingestion';
import { ExtendedMessage } from 'core/network/ingestion/ExtendedMessage';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { PublicKey } from 'core/objects';
import { dispatch } from 'core/redux';
import { WitnessMessage } from 'features/witness/network/messages';

import { Server } from '../objects/Server';
import { addServer } from '../reducer/ServerReducer';
import { GreetLao } from './messages/GreetLao';

export const handleLaoGreet = (greetLaoMsg: GreetLao, publicKey: PublicKey) => {
  dispatch(
    addServer(
      new Server({
        laoId: greetLaoMsg.lao,
        address: greetLaoMsg.address,
        serverPublicKey: publicKey,
        frontendPublicKey: greetLaoMsg.frontend,
      }).toState(),
    ),
  );

  // connect to all received peer addresses
  // after connecting to each peer, we will send a catchup which contains the lao#create message
  // as soon as we have parsed this message, we will connect to the LAO on the new connection as well
  // which will trigger another lao#greet message
  // IMPORTANT: The network manager deduplicates connections to the same address (string)
  // and the received peer addresses are supposed to be the canonical ones.
  // Hence we just have to make sure that the first connection is also to the canonical
  // address, otherwise a client will connect to the same server twice (e.g. using its IP and then
  // then using the canonical domain address)
  const networkManager = getNetworkManager();
  for (const peerAddress of greetLaoMsg.peers) {
    networkManager.connect(peerAddress.address);
  }
};

/**
 * Watches the redux store for new message#witness messages for lao#greet messages since they only
 * become valid after they are witnessed by the corresponding frontend
 * @remark Implemented analogous to 'makeMessageStoreWatcher'
 * @param store The redux store to watch
 * @param laoGreetSignatureHandler The function to call when a signature is added to a lao#greet message
 */
export const makeLaoGreetStoreWatcher = (
  store: Store,
  laoGreetSignatureHandler: (greetLaoMsg: GreetLao, publicKey: PublicKey) => void,
) => {
  let previousValue: string[] | undefined;
  let currentValue: string[] | undefined;
  return () => {
    const state = store.getState();
    const msgState = getMessagesState(state);
    const newValue = msgState?.unprocessedIds || [];
    [previousValue, currentValue] = [currentValue, newValue];

    if (
      previousValue !== undefined &&
      currentValue !== undefined &&
      previousValue.length === currentValue.length &&
      previousValue.every((value, index) => !currentValue || value === currentValue[index])
    ) {
      // no change detected, return immediately
      return;
    }

    // load new messages from the store
    const newMessages = currentValue.map((id: string) =>
      ExtendedMessage.fromState(msgState.byId[id]),
    );
    for (const witnessMessage of newMessages) {
      // only look at new messages of the type message#witness
      if (
        witnessMessage.messageData.object === ObjectType.MESSAGE &&
        witnessMessage.messageData.action === ActionType.WITNESS
      ) {
        const witnessMessageData = witnessMessage.messageData as WitnessMessage;

        // retrieve the corresponding message from the store
        const witnessedMessage = ExtendedMessage.fromState(
          msgState.byId[witnessMessageData.message_id.valueOf()],
        );

        if (
          witnessedMessage.messageData.object === ObjectType.LAO &&
          witnessedMessage.messageData.action === ActionType.GREET
        ) {
          // the newly witnessed message is of type lao#greet. check if the new signature comes from the corresponding frontend
          const laoGreetMessage = witnessedMessage.messageData as GreetLao;
          if (witnessMessage.sender.valueOf() === laoGreetMessage.frontend.valueOf()) {
            // if it is, then we can now treat this lao#greet message as being valid
            // it is only added to the store if the signature matches which means we do not
            // have to do this a second time
            laoGreetSignatureHandler(laoGreetMessage, witnessedMessage.sender);
          }
        }
      }
    }
  };
};
