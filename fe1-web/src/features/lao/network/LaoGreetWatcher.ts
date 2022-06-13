import { Store } from 'redux';

import { getNetworkManager } from 'core/network';
import { getMessagesState } from 'core/network/ingestion';
import { ExtendedMessage } from 'core/network/ingestion/ExtendedMessage';
import { Hash, PublicKey, WitnessSignatureState } from 'core/objects';
import { dispatch } from 'core/redux';

import { LaoServer } from '../objects/LaoServer';
import {
  getLaosState,
  handleGreetLaoMessage,
  selectCurrentLao,
  selectUnhandledGreetLaoWitnessSignaturesByMessageId,
} from '../reducer';
import { addServer } from "../reducer";
import { GreetLao } from './messages/GreetLao';

/**
 * Stores information about a given backend server based on a lao#greet message and connects to its peers
 * @param messageId The id of the greeting message
 * @param greetLaoMsg The GreetLao data of the greeting message
 * @param publicKey The public key of the message's sender, i.e. the public key of the backend
 */
export const storeBackendAndConnectToPeers = (
  messageId: Hash,
  greetLaoMsg: GreetLao,
  publicKey: PublicKey,
) => {
  dispatch(
    addServer(
      new LaoServer({
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

  // mark the lao#greet message as handled
  dispatch(
    handleGreetLaoMessage({
      messageId: messageId.valueOf(),
    }),
  );
};

/**
 * Watches the redux store for new message#witness messages for lao#greet messages since they only
 * become valid after they are witnessed by the corresponding frontend
 * @param store The redux store to watch
 * @param laoGreetSignatureHandler The function to call when the lao organizer's signature is added to a lao#greet message
 */
export const makeLaoGreetStoreWatcher = (
  store: Store,
  laoGreetSignatureHandler: (messageId: Hash, greetLaoMsg: GreetLao, publicKey: PublicKey) => void,
) => {
  let previousSignaturesByMessageId: { [messageId: string]: WitnessSignatureState[] } = {};

  return () => {
    const state = store.getState();
    const currentLao = selectCurrentLao(state);
    // we have to be careful with ExtendedMessage.fromState
    // since some message constructors assume that we are connected to a lao
    // thus we delay this watcher until we are connected to a lao
    if (!currentLao) {
      return;
    }

    // get the witness signatures for all unhandled lao#greet messages
    const signaturesByMessageId = selectUnhandledGreetLaoWitnessSignaturesByMessageId(state);

    // verify that the selector output has changed
    if (signaturesByMessageId === previousSignaturesByMessageId) {
      // if not, there won't be a new witness signature
      return;
    }

    // obtain the message state to retrieve the message data for a given message id
    const messageState = getMessagesState(state);
    // and the lao state to find the organizers key for a given lao id
    const laoState = getLaosState(state);

    // iterate over all messageIds
    for (const messageId of Object.keys(signaturesByMessageId)) {
      // check whether the signatures are different from the ones retrieved the last time
      if (signaturesByMessageId[messageId] !== previousSignaturesByMessageId[messageId]) {
        // if it is different, check if the lao organizer's key signed it
        const signatures = signaturesByMessageId[messageId];

        // for this, retrieve the message from the store
        if (!(messageId in messageState.byId)) {
          throw new Error(
            `The message with the id ${messageId} is stored int the greet lao reducer but could not be found in the message reducer`,
          );
        }
        const greetLaoMessage = ExtendedMessage.fromState(messageState.byId[messageId]);

        // as well as the key of the organizer of the lao corresponding to the message
        const laoId = greetLaoMessage.laoId?.valueOf();
        if (!laoId || !(laoId in laoState.byId)) {
          throw new Error(
            `The message with id ${messageId} was received from lao with id ${laoId} but this lao is not stored in the lao reducer`,
          );
        }
        const organizerFrontendPublicKey = laoState.byId[laoId].organizer;

        // check if the *new* set of signatures includes that of the organizer
        if (
          signatures.find(
            (signature) => signature.witness.valueOf() === organizerFrontendPublicKey.valueOf(),
          )
        ) {
          // if it does, call the callback
          laoGreetSignatureHandler(
            greetLaoMessage.message_id,
            greetLaoMessage.messageData as GreetLao,
            greetLaoMessage.sender,
          );
        }
      }
    }

    // remember the value for the next call to this function
    previousSignaturesByMessageId = signaturesByMessageId;
  };
};
