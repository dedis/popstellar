import { addMessageWitnessSignature } from 'core/network/ingestion';
import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { Hash, WitnessSignature } from 'core/objects';
import { dispatch, getStore } from 'core/redux';

import { selectLaosMap } from 'features/lao/reducer';

import { WitnessMessage } from './messages';

const getLao = (laoId: Hash | string) => selectLaosMap(getStore().getState())[laoId.valueOf()];

export function handleWitnessMessage(msg: ProcessableMessage): boolean {
  if (
    msg.messageData.object !== ObjectType.MESSAGE ||
    msg.messageData.action !== ActionType.WITNESS
  ) {
    console.warn('handleWitnessMessage was called to process an unsupported message', msg);
    return false;
  }

  const makeErr = (err: string) => `message/witness was not processed: ${err}`;

  const lao = getLao(msg.laoId);
  if (!lao) {
    console.warn(makeErr('LAO does not exist'));
    return false;
  }

  if (!lao.witnesses.includes(msg.sender)) {
    console.warn(makeErr('sender does not appear to be a valid witness'));
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

  dispatch(addMessageWitnessSignature(msgId, ws.toState()));

  return true;
}
