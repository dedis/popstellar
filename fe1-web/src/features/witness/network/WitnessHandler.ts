import { ExtendedMessage } from 'model/network/method/message';
import {
  ActionType,
  MessageRegistry,
  ObjectType,
  WitnessMessage,
} from 'model/network/method/message/data';
import { addMessageWitnessSignature, dispatch, getStore } from 'store';
import { makeCurrentLao } from 'features/lao/reducer';

import { WitnessSignature } from '../objects';

const getCurrentLao = makeCurrentLao();

function handleWitnessMessage(msg: ExtendedMessage): boolean {
  if (
    msg.messageData.object !== ObjectType.MESSAGE ||
    msg.messageData.action !== ActionType.WITNESS
  ) {
    console.warn('handleWitnessMessage was called to process an unsupported message', msg);
    return false;
  }

  const storeState = getStore().getState();
  const lao = getCurrentLao(storeState);
  if (!lao) {
    console.warn('message/witness was not processed: no LAO is currently active');
    return false;
  }

  const wsMsgData = msg.messageData as WitnessMessage;

  const msgId = wsMsgData.message_id;
  const ws = new WitnessSignature({
    witness: msg.sender,
    signature: wsMsgData.signature,
  });

  if (!ws.verify(msgId)) {
    console.warn(
      'Definitively ignoring witness message because ' +
        `signature by ${ws.witness} doesn't match message ${msgId}`,
      msg,
    );
    return true;
  }

  dispatch(addMessageWitnessSignature(lao.id, msgId, ws.toState()));

  return true;
}

/**
 * Configures the WitnessHandler in a MessageRegistry.
 *
 * @param registry - The MessageRegistry where we want to add the mapping
 */
export function configure(registry: MessageRegistry) {
  registry.addHandler(ObjectType.MESSAGE, ActionType.WITNESS, handleWitnessMessage);
}
