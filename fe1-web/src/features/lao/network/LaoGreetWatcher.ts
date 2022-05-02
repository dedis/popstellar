import { Store } from 'redux';

import { getNetworkManager } from 'core/network';
import { getMessagesState } from 'core/network/ingestion';
import { ExtendedMessage } from 'core/network/ingestion/ExtendedMessage';
import { PublicKey, WitnessSignatureState } from 'core/objects';
import { dispatch } from 'core/redux';

import { Server } from '../objects/Server';
import { getAllGreetLaoMessageIds, selectCurrentLao } from '../reducer';
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
 * @param store The redux store to watch
 * @param laoGreetSignatureHandler The function to call when a signature is added to a lao#greet message
 */
export const makeLaoGreetStoreWatcher = (
  store: Store,
  laoGreetSignatureHandler: (greetLaoMsg: GreetLao, publicKey: PublicKey) => void,
) => {
  let previousMessageIds: string[] | undefined;
  let currentMessageIds: string[] | undefined;

  const signaturesByMessageId: { [messageId: string]: WitnessSignatureState[] } = {};

  return () => {
    const state = store.getState();
    const lao = selectCurrentLao(state);
    // we have to be careful with ExtendedMessage.fromState
    // since some message constructors assume that we are connected to a lao
    // thus we delay this watcher until we are connected to a lao
    if (!lao) {
      return;
    }

    const messageState = getMessagesState(state);
    // these are the message ids for which we want to listen to changes
    // in the witness signatures
    const newValue = getAllGreetLaoMessageIds(state);
    [previousMessageIds, currentMessageIds] = [currentMessageIds, newValue];

    if (
      previousMessageIds !== undefined &&
      currentMessageIds !== undefined &&
      previousMessageIds.length === currentMessageIds.length &&
      previousMessageIds.every(
        (messageId, index) => !currentMessageIds || messageId === currentMessageIds[index],
      )
    ) {
      // no change detected, return immediately
      return;
    }

    currentMessageIds
      // filter messages to only get the new message ids
      .filter((messageId) => !(previousMessageIds || []).includes(messageId))
      // then store the empty set of witness signatures
      .forEach((messageId) => {
        signaturesByMessageId[messageId] = [];
      });

    // next iterate over all lao#greet messages we are keeping track of
    for (const messageId of Object.keys(signaturesByMessageId)) {
      // retrieve the set of witness signatures from the store
      const signatures = messageState.byId[messageId].witness_signatures;
      // and check whether they are different
      if (signaturesByMessageId[messageId] !== signatures) {
        const greetLaoMessage = ExtendedMessage.fromState(messageState.byId[messageId]);

        // retrieve the organizers public key for the corresponding lao
        const organizerFrontendPublicKey = lao.organizer;
        // if this is the case, check if the message now includes the signature of the organizer
        if (
          signatures.find(
            (signature) => signature.witness.valueOf() === organizerFrontendPublicKey.valueOf(),
          )
        ) {
          // if it does, call the callback
          laoGreetSignatureHandler(greetLaoMessage.messageData as GreetLao, greetLaoMessage.sender);
        } else {
          // store the new set of signatures preventing a new check until there
          signaturesByMessageId[messageId] = signatures;
        }
      }
    }
  };
};
