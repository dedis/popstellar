import { subscribeToChannel } from 'core/network';
import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { getReactionChannel, getUserSocialChannel, Hash } from 'core/objects';
import { AsyncDispatch, dispatch } from 'core/redux';

import { RollCallConfiguration } from '../interface';
import { RollCall, RollCallStatus } from '../objects';
import { CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall } from './messages';

/**
 * Handles a RollCallCreate message by creating a roll call in the current Lao.
 * @param addRollCall - A function to add a new roll call
 */
export const handleRollCallCreateMessage =
  (addRollCall: (laoId: Hash | string, rollCall: RollCall) => void) =>
  (msg: ProcessableMessage): boolean => {
    const makeErr = (err: string) => `roll_call#create was not processed: ${err}`;

    if (
      msg.messageData.object !== ObjectType.ROLL_CALL ||
      msg.messageData.action !== ActionType.CREATE
    ) {
      console.warn(
        makeErr(
          `Invalid object or action parameter: ${msg.messageData.object}#${msg.messageData.action}`,
        ),
      );
      return false;
    }

    if (!msg.laoId) {
      console.warn(makeErr(`Was not sent on a lao subchannel but rather on '${msg.channel}'`));
      return false;
    }

    const rcMsgData = msg.messageData as CreateRollCall;

    const rollCall = new RollCall({
      id: rcMsgData.id,
      name: rcMsgData.name,
      location: rcMsgData.location,
      description: rcMsgData.description,
      creation: rcMsgData.creation,
      proposedStart: rcMsgData.proposed_start,
      proposedEnd: rcMsgData.proposed_end,
      status: RollCallStatus.CREATED,
    });

    addRollCall(msg.laoId, rollCall);
    return true;
  };

/**
 * Handle a RollCallOpen message by opening the corresponding roll call.
 * @param getRollCallById - A function to get a roll call by its id
 * @param updateRollCall - An function to update a roll call
 */
export const handleRollCallOpenMessage =
  (
    getRollCallById: (rollCallId: Hash | string) => RollCall | undefined,
    updateRollCall: (rollCall: RollCall) => void,
  ) =>
  (msg: ProcessableMessage): boolean => {
    const makeErr = (err: string) => `roll_call#open was not processed: ${err}`;

    if (
      msg.messageData.object !== ObjectType.ROLL_CALL ||
      msg.messageData.action !== ActionType.OPEN
    ) {
      console.warn(
        makeErr(
          `Invalid object or action parameter: ${msg.messageData.object}#${msg.messageData.action}`,
        ),
      );
      return false;
    }

    if (!msg.laoId) {
      console.warn(makeErr(`Was not sent on a lao subchannel but rather on '${msg.channel}'`));
      return false;
    }

    const rcMsgData = msg.messageData as OpenRollCall;
    const oldRC = getRollCallById(rcMsgData.opens);
    if (!oldRC) {
      console.warn(makeErr("no known roll call matching the 'opens' field"));
      return false;
    }

    const rollCall = new RollCall({
      ...oldRC,
      idAlias: rcMsgData.update_id,
      openedAt: rcMsgData.opened_at,
      status: RollCallStatus.OPENED,
    });

    updateRollCall(rollCall);
    return true;
  };

/**
 * Handles a RollCallClose message by closing the corresponding roll call.
 * @param getRollCallById - A function to get a roll call by its id
 * @param updateRollCall - An function to update a roll call
 * @param generateToken - A function to generate a pop token
 * @param setLaoLastRollCall - An action creator to set the last (tokenized) roll call for a given lao
 */
export const handleRollCallCloseMessage =
  (
    getRollCallById: (rollCallId: Hash | string) => RollCall | undefined,
    updateRollCall: (rollCall: RollCall) => void,
    generateToken: RollCallConfiguration['generateToken'],
    setLaoLastRollCall: RollCallConfiguration['setLaoLastRollCall'],
  ) =>
  (msg: ProcessableMessage): boolean => {
    const makeErr = (err: string) => `roll_call#close was not processed: ${err}`;

    if (
      msg.messageData.object !== ObjectType.ROLL_CALL ||
      msg.messageData.action !== ActionType.CLOSE
    ) {
      console.warn(
        makeErr(
          `Invalid object or action parameter: ${msg.messageData.object}#${msg.messageData.action}`,
        ),
      );
      return false;
    }

    if (!msg.laoId) {
      console.warn(makeErr(`Was not sent on a lao subchannel but rather on '${msg.channel}'`));
      return false;
    }

    const { laoId } = msg;

    const rcMsgData = msg.messageData as CloseRollCall;
    const oldRC = getRollCallById(rcMsgData.closes);
    if (!oldRC) {
      console.warn(makeErr("no known roll call matching the 'closes' field"));
      return false;
    }

    const rollCall = new RollCall({
      ...oldRC,
      idAlias: rcMsgData.update_id,
      closedAt: rcMsgData.closed_at,
      status: RollCallStatus.CLOSED,
      attendees: rcMsgData.attendees,
    });

    updateRollCall(rollCall);

    // ... and update the Lao state to point to the latest roll call, if we have a token in it.
    dispatch(async (aDispatch: AsyncDispatch) => {
      try {
        const token = await generateToken(laoId, rollCall.id);
        const hasToken = rollCall.containsToken(token);
        aDispatch(setLaoLastRollCall(laoId, rollCall.id, hasToken));

        // If we had a token in this roll call, we subscribe to our own social media channel
        if (token && hasToken) {
          await subscribeToChannel(
            laoId,
            dispatch,
            getUserSocialChannel(laoId, token.publicKey),
          ).catch((err) => {
            console.error(
              `Could not subscribe to our own social channel ${token.publicKey}, error:`,
              err,
            );
          });
        }
        // everyone is automatically subscribed to the reaction channel after the roll call
        await subscribeToChannel(laoId, dispatch, getReactionChannel(laoId)).catch((err) => {
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
 * @param getRollCallById - A function to get a roll call by its id
 * @param updateRollCall - An function to update a roll call
 */
export const handleRollCallReopenMessage =
  (
    getRollCallById: (rollCallId: Hash | string) => RollCall | undefined,
    updateRollCall: (rollCall: RollCall) => void,
  ) =>
  (msg: ProcessableMessage) => {
    const makeErr = (err: string) => `roll_call#reopen was not processed: ${err}`;

    if (
      msg.messageData.object !== ObjectType.ROLL_CALL ||
      msg.messageData.action !== ActionType.REOPEN
    ) {
      console.warn(
        makeErr(
          `Invalid object or action parameter: ${msg.messageData.object}#${msg.messageData.action}`,
        ),
      );
      return false;
    }

    if (!msg.laoId) {
      console.warn(makeErr(`Was not sent on a lao subchannel but rather on '${msg.channel}'`));
      return false;
    }

    const rcMsgData = msg.messageData as ReopenRollCall;
    const oldRC = getRollCallById(rcMsgData.opens);
    if (!oldRC) {
      console.warn(makeErr("no known roll call matching the 'opens' field"));
      return false;
    }
    if (oldRC.status !== RollCallStatus.CLOSED) {
      console.error(makeErr('The roll call status is not coherent'));
      return false;
    }

    const rollCall = new RollCall({
      ...oldRC,
      idAlias: rcMsgData.update_id,
      openedAt: rcMsgData.opened_at,
      status: RollCallStatus.REOPENED,
    });

    updateRollCall(rollCall);
    return true;
  };
