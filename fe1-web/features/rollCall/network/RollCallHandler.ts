import { ExtendedMessage } from 'model/network/method/message';
import { ActionType, MessageRegistry, ObjectType } from 'model/network/method/message/data';
import { getReactionChannel, getUserSocialChannel } from 'model/objects';
import {
  AsyncDispatch,
  dispatch,
  getStore,
  makeCurrentLao,
  setLaoLastRollCall,
} from 'store';
import { subscribeToChannel } from 'network/CommunicationApi';
import { addEvent, updateEvent } from 'features/events/reducer/EventsReducer';
import { getEventFromId } from 'features/events/network/EventHandlerUtils';
import * as Wallet from 'features/wallet/objects';

import { CloseRollCall, CreateRollCall, OpenRollCall } from './messages';
import { RollCall, RollCallStatus } from '../objects';

/**
 * Handles all incoming roll call messages.
 */

const getCurrentLao = makeCurrentLao();

/**
 * Handles a RollCallCreate message by creating a roll call in the current Lao.
 *
 * @param msg - The extended message for creating a roll call
 */
function handleRollCallCreateMessage(msg: ExtendedMessage): boolean {
  if (msg.messageData.object !== ObjectType.ROLL_CALL
    || msg.messageData.action !== ActionType.CREATE) {
    console.warn('handleRollCallCreateMessage was called to process an unsupported message', msg);
    return false;
  }

  const makeErr = (err: string) => `roll_call/create was not processed: ${err}`;

  const storeState = getStore().getState();
  const lao = getCurrentLao(storeState);
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
function handleRollCallOpenMessage(msg: ExtendedMessage): boolean {
  if (msg.messageData.object !== ObjectType.ROLL_CALL
    || msg.messageData.action !== ActionType.OPEN) {
    console.warn('handleRollCallOpenMessage was called to process an unsupported message', msg);
    return false;
  }

  const makeErr = (err: string) => `roll_call/open was not processed: ${err}`;

  const storeState = getStore().getState();
  const lao = getCurrentLao(storeState);
  if (!lao) {
    console.warn(makeErr('no LAO is currently active'));
    return false;
  }

  const rcMsgData = msg.messageData as OpenRollCall;
  const oldRC = getEventFromId(storeState, rcMsgData.opens) as RollCall;
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
function handleRollCallCloseMessage(msg: ExtendedMessage): boolean {
  if (msg.messageData.object !== ObjectType.ROLL_CALL
    || msg.messageData.action !== ActionType.CLOSE) {
    console.warn('handleRollCallCloseMessage was called to process an unsupported message', msg);
    return false;
  }

  const makeErr = (err: string) => `roll_call/close was not processed: ${err}`;

  const storeState = getStore().getState();
  const lao = getCurrentLao(storeState);
  if (!lao) {
    console.warn(makeErr('no LAO is currently active'));
    return false;
  }

  const rcMsgData = msg.messageData as CloseRollCall;
  const oldRC = getEventFromId(storeState, rcMsgData.closes) as RollCall;
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
        await subscribeToChannel(getUserSocialChannel(lao.id, token.publicKey))
          .catch((err) => {
            console.error(`Could not subscribe to our own social channel ${token.publicKey}, error:`,
              err);
          });
      }
      // everyone is automatically subscribed to the reaction channel after the roll call
      await subscribeToChannel(getReactionChannel(lao.id))
        .catch((err) => {
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
function handleRollCallReopenMessage(msg: ExtendedMessage) {
  console.warn('A RollCall reopen message was received but'
    + ' its processing logic is not yet implemented:', msg);
  return false;
}

/**
 * Configures the RollCallHandler in a MessageRegistry.
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configure(registry: MessageRegistry) {
  registry.addHandler(ObjectType.ROLL_CALL, ActionType.CREATE, handleRollCallCreateMessage);
  registry.addHandler(ObjectType.ROLL_CALL, ActionType.OPEN, handleRollCallOpenMessage);
  registry.addHandler(ObjectType.ROLL_CALL, ActionType.CLOSE, handleRollCallCloseMessage);
  registry.addHandler(ObjectType.ROLL_CALL, ActionType.REOPEN, handleRollCallReopenMessage);
}
