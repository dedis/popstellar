import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { dispatch } from 'core/redux';

import { LinkedOrganizationsCompositionConfiguration } from '../interface';
import { ChallengeRequest } from './messages/ChallengeRequest';
import { Challenge, ChallengeState } from '../objects/Challenge';
import { requestChallenge } from './LinkedOrgMessageApi';
import { addChallenge } from '../reducer';
import { ChallengeMessage } from './messages/ChallengeMessage';

/**
 * Handler for linked organization messages
 */

/**
 * Handles an ChallengeRequest message.
 */
export const handleChallengeMessage =
  (getLaoOrganizerBackendPublicKey: LinkedOrganizationsCompositionConfiguration['getLaoOrganizerBackendPublicKey']) => (msg: ProcessableMessage) => {
    if (msg.messageData.object !== ObjectType.FEDERATION || msg.messageData.action !== ActionType.CHALLENGE) {
      console.warn('handleRequestChallengeMessage was called to process an unsupported message');
      return false;
    }

    const makeErr = (err: string) => `challenge was not processed: ${err}`;

    // obtain the lao id from the channel
    if (!msg.laoId) {
      console.warn(makeErr('message was not sent on a lao subchannel'));
      return false;
    }

    const challengeMessage = msg.messageData as ChallengeMessage;
    /*
    // for now *ALL* election#key messages *MUST* be sent by the backend of the organizer
    const organizerBackendPublicKey = getLaoOrganizerBackendPublicKey(msg.laoId);

    if (!organizerBackendPublicKey) {
      console.warn(makeErr("the organizer backend's public key is unknown"));
      return false;
    }

    if (organizerBackendPublicKey.valueOf() !== msg.sender.valueOf()) {
      console.warn(makeErr("the senders' public key does not match the organizer backend's"));
      return false;
    }*/

    const challengeState: ChallengeState = {
      value: challengeMessage.value.toState(),
      valid_until: challengeMessage.valid_until,
    };

    dispatch(addChallenge(msg.laoId, challengeState));
    return true;
  };


/**
 * Handles an requestChallenge message.
 */
export const handleChallengeRequestMessage =
  (getCurrentLaoId: LinkedOrganizationsCompositionConfiguration['getCurrentLaoId']) => (msg: ProcessableMessage) => {
    if (
      msg.messageData.object !== ObjectType.FEDERATION ||
      msg.messageData.action !== ActionType.CHALLENGE_REQUEST
    ) {
      console.warn('handleRequestChallengeMessage was called to process an unsupported message');
      return false;
    }

    const makeErr = (err: string) => `challenge/request was not processed: ${err}`;

    const laoId = getCurrentLaoId();
    if (!laoId) {
      console.warn(makeErr('no Lao is currently active'));
      return false;
    }

    const messageId = msg.message_id;
    const { sender } = msg;
    const requestChallenge = msg.messageData as ChallengeRequest;

    return true;
  };

