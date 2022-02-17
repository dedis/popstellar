import { ExtendedMessage } from 'model/network/method/message';
import { ActionType, ObjectType, WitnessMessage } from 'model/network/method/message/data';
import { WitnessSignature } from 'model/objects';
import { dispatch, addMessageWitnessSignature, getStore, makeCurrentLao } from 'store';

const getCurrentLao = makeCurrentLao();

export function handleWitnessMessage(msg: ExtendedMessage): boolean {
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
