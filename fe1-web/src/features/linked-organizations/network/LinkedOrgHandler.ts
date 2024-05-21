import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { dispatch } from 'core/redux';

import { LinkedOrganizationsCompositionConfiguration } from '../interface';
import { Challenge } from '../objects/Challenge';
import { addChallenge } from '../reducer';
import { ChallengeMessage } from './messages/ChallengeMessage';
import { ChallengeRequest } from './messages';
import { FederationInit } from './messages/FederationInit';
import { FederationExpect } from './messages/FederationExpect';

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
    try {
      const challengeRequest = msg.messageData as ChallengeRequest;
  
      const jsonObj = {
        timestamp: challengeRequest.timestamp.valueOf(),
      };
      return true;
    } catch {
      return false;
    }
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

    try {
      const federationInit = msg.messageData as FederationInit;
  
      const jsonObj = {
        lao_id: federationInit.lao_id.valueOf(),
        public_key: federationInit.public_key.valueOf(),
        server_address: federationInit.server_address,
        challenge: federationInit.challenge,
      };
      return true;
    } catch {
      return false;
    }
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

    try {
      const federationExpect = msg.messageData as FederationExpect;
  
      const jsonObj = {
        lao_id: federationExpect.lao_id.valueOf(),
        public_key: federationExpect.public_key.valueOf(),
        server_address: federationExpect.server_address,
        challenge: federationExpect.challenge,
      };
      return true;
    } catch {
      return false;
    }
  };
