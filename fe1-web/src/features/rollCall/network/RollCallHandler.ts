import { subscribeToChannel } from 'core/network';
import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { getReactionChannel, getUserSocialChannel } from 'core/objects';
import { AsyncDispatch, dispatch } from 'core/redux';

import { RollCallConfiguration } from '../interface';
import { RollCall, RollCallStatus } from '../objects';
import { CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall } from './messages';

/**
 * Handles a RollCallCreate message by creating a roll call in the current Lao.
 *
 * @param addEvent - An action creator to add a new lao event
 */
export const handleRollCallCreateMessage =
  (addEvent: RollCallConfiguration['addEvent']) =>
  (msg: ProcessableMessage): boolean => {
    if (
      msg.messageData.object !== ObjectType.ROLL_CALL ||
      msg.messageData.action !== ActionType.CREATE
    ) {
      console.warn('handleRollCallCreateMessage was called to process an unsupported message', msg);
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

    dispatch(addEvent(msg.laoId, rc.toState()));
    return true;
  };

/**
 * Handle a RollCallOpen message by opening the corresponding roll call.
 *
 * @param getEventById - A function to get an event by its id
 * @param updateEvent - An action creator to update a lao event
 */
export const handleRollCallOpenMessage =
  (
    getEventById: RollCallConfiguration['getEventById'],
    updateEvent: RollCallConfiguration['updateEvent'],
  ) =>
  (msg: ProcessableMessage): boolean => {
    if (
      msg.messageData.object !== ObjectType.ROLL_CALL ||
      msg.messageData.action !== ActionType.OPEN
    ) {
      console.warn('handleRollCallOpenMessage was called to process an unsupported message', msg);
      return false;
    }

    const makeErr = (err: string) => `roll_call/open was not processed: ${err}`;

    const rcMsgData = msg.messageData as OpenRollCall;
    const oldRC = getEventById(rcMsgData.opens) as RollCall;
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

    dispatch(updateEvent(msg.laoId, rc.toState()));
    return true;
  };

/**
 * Handles a RollCallClose message by closing the corresponding roll call.
 *
 * @param getEventById - A function to get an event by its id
 * @param updateEvent - An action creator to update a lao event
 * @param generateToken - A function to generate a pop token
 * @param setLaoLastRollCall - An action creator to set the last (tokenized) roll call for a given lao
 */
export const handleRollCallCloseMessage =
  (
    getEventById: RollCallConfiguration['getEventById'],
    updateEvent: RollCallConfiguration['updateEvent'],
    generateToken: RollCallConfiguration['generateToken'],
    setLaoLastRollCall: RollCallConfiguration['setLaoLastRollCall'],
  ) =>
  (msg: ProcessableMessage): boolean => {
    if (
      msg.messageData.object !== ObjectType.ROLL_CALL ||
      msg.messageData.action !== ActionType.CLOSE
    ) {
      console.warn('handleRollCallCloseMessage was called to process an unsupported message', msg);
      return false;
    }

    const makeErr = (err: string) => `roll_call/close was not processed: ${err}`;

    const rcMsgData = msg.messageData as CloseRollCall;
    const oldRC = getEventById(rcMsgData.closes) as RollCall;
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
    dispatch(updateEvent(msg.laoId, rc.toState()));

    // ... and update the Lao state to point to the latest roll call, if we have a token in it.
    dispatch(async (aDispatch: AsyncDispatch) => {
      try {
        const token = await generateToken(msg.laoId, rc.id);
        const hasToken = rc.containsToken(token);
        aDispatch(setLaoLastRollCall(msg.laoId, rc.id, hasToken));

        // If we had a token in this roll call, we subscribe to our own social media channel
        if (token && hasToken) {
          await subscribeToChannel(getUserSocialChannel(msg.laoId, token.publicKey)).catch(
            (err) => {
              console.error(
                `Could not subscribe to our own social channel ${token.publicKey}, error:`,
                err,
              );
            },
          );
        }
        // everyone is automatically subscribed to the reaction channel after the roll call
        await subscribeToChannel(getReactionChannel(msg.laoId)).catch((err) => {
          console.error('Could not subscribe to reaction channel, error:', err);
        });
      } catch (err) {
        console.debug(err);
      }
    });

    return true;
  };

/**
 * Handles a RollCallReopen message by reopening the corresponding roll call.
 *
 * @param getEventById - A function to get an event by its id
 * @param updateEvent - An action creator to update a lao event
 */
export const handleRollCallReopenMessage =
  (
    getEventById: RollCallConfiguration['getEventById'],
    updateEvent: RollCallConfiguration['updateEvent'],
  ) =>
  (msg: ProcessableMessage) => {
    if (
      msg.messageData.object !== ObjectType.ROLL_CALL ||
      msg.messageData.action !== ActionType.REOPEN
    ) {
      console.warn('handleRollCallReopenMessage was called to process an unsupported message', msg);
      return false;
    }

    const makeErr = (err: string) => `roll_call/reopen was not processed: ${err}`;

    const rcMsgData = msg.messageData as ReopenRollCall;
    const oldRC = getEventById(rcMsgData.opens) as RollCall;
    if (!oldRC) {
      console.warn(makeErr("no known roll call matching the 'opens' field"));
      return false;
    }
    if (oldRC.status !== RollCallStatus.CLOSED) {
      console.error(makeErr('The roll call status is not coherent'));
      return false;
    }

    const rc = new RollCall({
      ...oldRC,
      idAlias: rcMsgData.update_id,
      openedAt: rcMsgData.opened_at,
      status: RollCallStatus.REOPENED,
    });

    dispatch(updateEvent(msg.laoId, rc.toState()));
    return true;
  };
