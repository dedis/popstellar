import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { dispatch } from 'core/redux';

import { Lao } from '../objects';
import { addUnhandledGreetLaoMessage, connectToLao } from '../reducer';
import { storeBackendAndConnectToPeers } from './LaoGreetWatcher';
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

  // add the lao#greet message to the store to tell LaoGreetWatcher to keep an eye on it
  dispatch(
    addUnhandledGreetLaoMessage({
      messageId: msg.message_id.valueOf(),
    }),
  );

  // only treat the message as being valid when it is signed by the advertised frontend public key
  if (
    !msg.witness_signatures.find((witnessSignature) =>
      witnessSignature.signature.verify(greetLaoMsg.frontend, msg.message_id),
    )
  ) {
    // lao#greet message has not (yet) been signed by the corresponding frontend
    // wait for the signature in LaoGreetWatcher
    // FIXME: for now the witnessing feature is not working and thus we omit this check (2022-04-25, Tyratox)
    // moreover we decided in the evoting meeting to add this check only later and focus on the other parts for now
    // return true;
  }

  storeBackendAndConnectToPeers(msg.message_id, greetLaoMsg, msg.sender);

  return true;
};

export function handleLaoUpdatePropertiesMessage(msg: ProcessableMessage): boolean {
  console.debug(`lao/update_properties message was archived: no action needs to be taken ${msg}`);
  return true;
}
