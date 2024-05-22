import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { dispatch } from 'core/redux';

import { LinkedOrganizationsCompositionConfiguration } from '../interface';
import { Challenge } from '../objects/Challenge';
import { addChallenge } from '../reducer';
import { ChallengeRequest } from './messages';
import { ChallengeMessage } from './messages/ChallengeMessage';
import { FederationExpect } from './messages/FederationExpect';
import { FederationInit } from './messages/FederationInit';

/**
 * Handler for linked organization messages
 */

/**
 * Handles an ChallengeRequest message.
 */
export const handleChallengeMessage = () => (msg: ProcessableMessage) => {
  if (
    msg.messageData.object !== ObjectType.FEDERATION ||
    msg.messageData.action !== ActionType.CHALLENGE
  ) {
    console.warn('handleRequestChallengeMessage was called to process an unsupported message');
    return false;
  }

  const makeErr = (err: string) => `challenge was not processed: ${err}`;

  // obtain the lao id from the channel
  if (!msg.laoId) {
    console.warn(makeErr('message was not sent on a lao subchannel'));
    return false;
  }
  try {
    const challengeMessage = msg.messageData as ChallengeMessage;

    const jsonObj = {
      value: challengeMessage.value.toString(),
      valid_until: challengeMessage.valid_until,
    };
    const challenge = Challenge.fromJson(jsonObj);
    dispatch(addChallenge(msg.laoId, challenge.toState()));
    return true;
  } catch {
    return false;
  }
};

/**
 * Handles an requestChallenge message.
 */
export const handleChallengeRequestMessage =
  (getCurrentLaoId: LinkedOrganizationsCompositionConfiguration['getCurrentLaoId']) =>
  (msg: ProcessableMessage) => {
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
    if (msg.messageData instanceof ChallengeRequest) {
      const challengeRequest = msg.messageData as ChallengeRequest;
      if (challengeRequest.timestamp) {
        return true;
      }
    }
    return false;
  };

/**
 * Handles an federationInit message.
 */
export const handleFederationInitMessage =
  (getCurrentLaoId: LinkedOrganizationsCompositionConfiguration['getCurrentLaoId']) =>
  (msg: ProcessableMessage) => {
    if (
      msg.messageData.object !== ObjectType.FEDERATION ||
      msg.messageData.action !== ActionType.FEDERATION_INIT
    ) {
      console.warn('handleFederationInitMessage was called to process an unsupported message');
      return false;
    }

    const makeErr = (err: string) => `federation/init was not processed: ${err}`;

    const laoId = getCurrentLaoId();
    if (!laoId) {
      console.warn(makeErr('no Lao is currently active'));
      return false;
    }

    if (msg.messageData instanceof FederationInit) {
      const federationInit = msg.messageData as FederationInit;
      if (
        federationInit.lao_id &&
        federationInit.public_key &&
        federationInit.server_address &&
        federationInit.challenge
      ) {
        return true;
      }
    }
    return false;
  };

/**
 * Handles an federationExpect message.
 */
export const handleFederationExpectMessage =
  (getCurrentLaoId: LinkedOrganizationsCompositionConfiguration['getCurrentLaoId']) =>
  (msg: ProcessableMessage) => {
    if (
      msg.messageData.object !== ObjectType.FEDERATION ||
      msg.messageData.action !== ActionType.FEDERATION_EXPECT
    ) {
      console.warn('handleFederationExpectMessage was called to process an unsupported message');
      return false;
    }

    const makeErr = (err: string) => `federation/expect was not processed: ${err}`;

    const laoId = getCurrentLaoId();
    if (!laoId) {
      console.warn(makeErr('no Lao is currently active'));
      return false;
    }

    if (msg.messageData instanceof FederationExpect) {
      const federationExpect = msg.messageData as FederationExpect;
      if (
        federationExpect.lao_id &&
        federationExpect.public_key &&
        federationExpect.server_address &&
        federationExpect.challenge
      ) {
        return true;
      }
    }
    return false;
  };
