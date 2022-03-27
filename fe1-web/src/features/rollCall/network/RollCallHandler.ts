import { subscribeToChannel } from 'core/network';
import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { getReactionChannel, getUserSocialChannel } from 'core/objects';
import { AsyncDispatch, dispatch, getStore } from 'core/redux';
import { selectEventById } from 'features/events/network/EventHandlerUtils';
import { addEvent, updateEvent } from 'features/events/reducer';
import { selectCurrentLao, setLaoLastRollCall } from 'features/lao/reducer';
import * as Wallet from 'features/wallet/objects';

import { RollCall, RollCallStatus } from '../objects';
import { CloseRollCall, CreateRollCall, OpenRollCall } from './messages';

/**
 * Handles all incoming roll call messages.
 */

/**
 * Handles a RollCallCreate message by creating a roll call in the current Lao.
 *
 * @param msg - The extended message for creating a roll call
 */
export function handleRollCallCreateMessage(msg: ProcessableMessage): boolean {
  if (
    msg.messageData.object !== ObjectType.ROLL_CALL ||
    msg.messageData.action !== ActionType.CREATE
  ) {
    console.warn('handleRollCallCreateMessage was called to process an unsupported message', msg);
    return false;
  }

  const makeErr = (err: string) => `roll_call/create was not processed: ${err}`;

  const storeState = getStore().getState();
  const lao = selectCurrentLao(storeState);
  if (!lao) {
    console.warn(makeErr('no LAO is currently active'));
    return false;
  }

  const rcMsgData = msg.messageData as CreateRollCall;

  const rc = new RollCall({
    id: rcMsgData.id,
    name: rcMsgData.name,
    location: rcMsgData.location,
    description: rcMsgData.description,
    creation: rcMsgData.creation,
    proposedStart: rcMsgData.proposed_start,
    proposedEnd: rcMsgData.proposed_end,
    status: RollCallStatus.CREATED,
  });

  dispatch(addEvent(lao.id, rc.toState()));
  return true;
}

/**
 * Handle a RollCallOpen message by opening the corresponding roll call.
 *
 * @param msg - The extended message for opening a roll call
 */
export function handleRollCallOpenMessage(msg: ProcessableMessage): boolean {
  if (
    msg.messageData.object !== ObjectType.ROLL_CALL ||
    msg.messageData.action !== ActionType.OPEN
  ) {
    console.warn('handleRollCallOpenMessage was called to process an unsupported message', msg);
    return false;
  }

  const makeErr = (err: string) => `roll_call/open was not processed: ${err}`;

  const storeState = getStore().getState();
  const lao = selectCurrentLao(storeState);
  if (!lao) {
    console.warn(makeErr('no LAO is currently active'));
    return false;
  }

  const rcMsgData = msg.messageData as OpenRollCall;
  const oldRC = selectEventById(storeState, rcMsgData.opens) as RollCall;
  if (!oldRC) {
    console.warn(makeErr("no known roll call matching the 'opens' field"));
    return false;
  }

  const rc = new RollCall({
    ...oldRC,
    idAlias: rcMsgData.update_id,
    openedAt: rcMsgData.opened_at,
    status: RollCallStatus.OPENED,
  });

  dispatch(updateEvent(lao.id, rc.toState()));
  return true;
}

/**
 * Handles a RollCallClose message by closing the corresponding roll call.
 *
 * @param msg - The extended message for closing a roll call
 */
export function handleRollCallCloseMessage(msg: ProcessableMessage): boolean {
  if (
    msg.messageData.object !== ObjectType.ROLL_CALL ||
    msg.messageData.action !== ActionType.CLOSE
  ) {
    console.warn('handleRollCallCloseMessage was called to process an unsupported message', msg);
    return false;
  }

  const makeErr = (err: string) => `roll_call/close was not processed: ${err}`;

  const storeState = getStore().getState();
  const lao = selectCurrentLao(storeState);
  if (!lao) {
    console.warn(makeErr('no LAO is currently active'));
    return false;
  }

  const rcMsgData = msg.messageData as CloseRollCall;
  const oldRC = selectEventById(storeState, rcMsgData.closes) as RollCall;
  if (!oldRC) {
    console.warn(makeErr("no known roll call matching the 'closes' field"));
    return false;
  }

  const rc = new RollCall({
    ...oldRC,
    idAlias: rcMsgData.update_id,
    closedAt: rcMsgData.closed_at,
    status: RollCallStatus.CLOSED,
    attendees: rcMsgData.attendees,
  });

  // We can now dispatch an updated (closed) roll call, containing the attendees' public keys.
  dispatch(updateEvent(lao.id, rc.toState()));

  // ... and update the Lao state to point to the latest roll call, if we have a token in it.
  dispatch(async (aDispatch: AsyncDispatch) => {
    try {
      const token = await Wallet.generateToken(lao.id, rc.id);
      const hasToken = rc.containsToken(token);
      aDispatch(setLaoLastRollCall(lao.id, rc.id, hasToken));

      // If we had a token in this roll call, we subscribe to our own social media channel
      if (token && hasToken) {
        await subscribeToChannel(getUserSocialChannel(lao.id, token.publicKey)).catch((err) => {
          console.error(
            `Could not subscribe to our own social channel ${token.publicKey}, error:`,
            err,
          );
        });
      }
      // everyone is automatically subscribed to the reaction channel after the roll call
      await subscribeToChannel(getReactionChannel(lao.id)).catch((err) => {
        console.error('Could not subscribe to reaction channel, error:', err);
      });
    } catch (err) {
      console.debug(err);
    }
  });

  return true;
}

/**
 * TODO: Handles a reopen roll call message.
 *
 * @param msg
 */
export function handleRollCallReopenMessage(msg: ProcessableMessage) {
  console.warn(
    'A RollCall reopen message was received but its processing logic is not yet implemented:',
    msg,
  );
  return false;
}
