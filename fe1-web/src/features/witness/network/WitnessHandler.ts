import { addMessageWitnessSignature } from 'core/network/ingestion';
import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { WitnessSignature } from 'core/objects';
import { dispatch } from 'core/redux';

import { WitnessConfiguration } from '../interface';
import { WitnessMessage } from './messages';

export const handleWitnessMessage =
  (getCurrentLao: WitnessConfiguration['getCurrentLao']) =>
  (msg: ProcessableMessage): boolean => {
    if (
      msg.messageData.object !== ObjectType.MESSAGE ||
      msg.messageData.action !== ActionType.WITNESS
    ) {
      console.warn('handleWitnessMessage was called to process an unsupported message', msg);
      return false;
    }

    const makeErr = (err: string) => `message/witness was not processed: ${err}`;

    const lao = getCurrentLao();
    if (msg.laoId && lao.id.valueOf() !== msg.laoId.valueOf()) {
      console.warn(
        makeErr(
          `lao id of the received message (${msg.laoId.valueOf()}) does not match the current lao id (${lao.id.valueOf()})`,
        ),
      );
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
      return false;
    }

    dispatch(addMessageWitnessSignature(msgId, ws.toState()));

    return true;
  };
