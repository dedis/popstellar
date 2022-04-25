import { getNetworkManager } from 'core/network';
import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { dispatch } from 'core/redux';

import { Lao } from '../objects';
import { Server } from '../objects/Server';
import { connectToLao } from '../reducer';
import { addServer } from '../reducer/ServerReducer';
import { CreateLao } from './messages';
import { GreetLao } from './messages/GreetLao';

export function handleLaoCreateMessage(msg: ProcessableMessage): boolean {
  if (msg.messageData.object !== ObjectType.LAO || msg.messageData.action !== ActionType.CREATE) {
    console.warn('handleLaoCreateMessage was called to process an unsupported message', msg);
    return false;
  }

  const createLaoMsg = msg.messageData as CreateLao;
  const lao = new Lao({
    id: createLaoMsg.id,
    name: createLaoMsg.name,
    creation: createLaoMsg.creation,
    last_modified: createLaoMsg.creation,
    organizer: createLaoMsg.organizer,
    witnesses: createLaoMsg.witnesses,
    server_addresses: [msg.receivedFrom],
  });

  dispatch(connectToLao(lao.toState()));
  return true;
}

export function handleLaoStateMessage(msg: ProcessableMessage): boolean {
  if (msg.messageData.object !== ObjectType.LAO || msg.messageData.action !== ActionType.STATE) {
    console.warn('handleLaoStateMessage was called to process an unsupported message', msg);
    return false;
  }

  const makeErr = (err: string) => `lao/state was not processed: ${err}`;

  console.warn(makeErr('currently unsupported, needs redesign with consensus'));
  return true;
  /*
  const storeState = getStore().getState();
  const oldLao = getCurrentLao(storeState);
  if (!oldLao) {
    console.warn(makeErr('no LAO is currently active'));
    return false;
  }

  const stateLaoData = msg.messageData as StateLao;
  if (!hasWitnessSignatureQuorum(stateLaoData.modification_signatures, oldLao)) {
    console.warn(makeErr('witness quorum was not reached'));
    return false;
  }

  const msgState = getMessageState(storeState);
  if (!msgState) {
    console.warn(makeErr("no known 'lao/update_properties' messages in LAO"));
    return false;
  }

  const updateMessage = getMessage(msgState, stateLaoData.modification_id);
  if (!updateMessage) {
    console.warn(makeErr("'modification_id' references unknown message"));
    return false;
  }
  const updateLaoData = updateMessage.messageData as UpdateLao;

  const lao = new Lao({
    ...oldLao,
    name: updateLaoData.name,
    witnesses: updateLaoData.witnesses,
  });

  dispatch(updateLao(lao.toState()));
  return true; */
}

export const handleLaoGreetMessage = (msg: ProcessableMessage): boolean => {
  if (msg.messageData.object !== ObjectType.LAO || msg.messageData.action !== ActionType.GREET) {
    console.warn('handleLaoGreetMessage was called to process an unsupported message', msg);
    return false;
  }

  const greetLaoMsg = msg.messageData as GreetLao;

  dispatch(
    addServer(
      new Server({
        laoId: greetLaoMsg.lao,
        address: greetLaoMsg.address,
        publicKey: greetLaoMsg.sender,
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
  return true;
};

export function handleLaoUpdatePropertiesMessage(msg: ProcessableMessage): boolean {
  console.debug(`lao/update_properties message was archived: no action needs to be taken ${msg}`);
  return true;
}
