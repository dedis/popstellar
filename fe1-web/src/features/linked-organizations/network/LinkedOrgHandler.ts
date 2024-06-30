import { subscribeToChannel } from 'core/network';
import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { Base64UrlData, getReactionChannel, getUserSocialChannel } from 'core/objects';
import { dispatch } from 'core/redux';

import { LinkedOrganizationsConfiguration } from '../interface';
import { Challenge } from '../objects/Challenge';
import { addReceivedChallenge, setChallenge } from '../reducer';
import { addLinkedLaoId } from '../reducer/LinkedOrganizationsReducer';
import {
  ChallengeRequest,
  ChallengeMessage,
  FederationExpect,
  FederationInit,
  FederationResult,
} from './messages';
import { TokensExchange } from './messages/TokensExchange';

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
    dispatch(setChallenge(msg.laoId, challenge.toState()));
    return true;
  } catch {
    return false;
  }
};

/**
 * Handles an requestChallenge message.
 */
export const handleChallengeRequestMessage =
  (getCurrentLaoId: LinkedOrganizationsConfiguration['getCurrentLaoId']) =>
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
  (getCurrentLaoId: LinkedOrganizationsConfiguration['getCurrentLaoId']) =>
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
  (getCurrentLaoId: LinkedOrganizationsConfiguration['getCurrentLaoId']) =>
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

/**
 * Handles an federationResult message.
 */
export const handleFederationResultMessage =
  (getCurrentLaoId: LinkedOrganizationsConfiguration['getCurrentLaoId']) =>
  (msg: ProcessableMessage) => {
    if (
      msg.messageData.object !== ObjectType.FEDERATION ||
      msg.messageData.action !== ActionType.FEDERATION_RESULT
    ) {
      console.warn('handleFederationResultMessage was called to process an unsupported message');
      return false;
    }
    const makeErr = (err: string) => `federation/result was not processed: ${err}`;

    const laoId = getCurrentLaoId();
    if (!laoId) {
      console.warn(makeErr('no Lao is currently active'));
      return false;
    }

    if (msg.messageData instanceof FederationResult) {
      const federationResult = msg.messageData as FederationResult;
      try {
        if (
          federationResult.status &&
          federationResult.challenge &&
          (federationResult.reason || federationResult.public_key)
        ) {
          const b64urldata = new Base64UrlData(federationResult.challenge.data.toString());
          const js = JSON.parse(b64urldata.decode());
          const challengeMessage = ChallengeMessage.fromJson(js);
          const challenge = new Challenge({
            value: challengeMessage.value,
            valid_until: challengeMessage.valid_until,
          });
          dispatch(addReceivedChallenge(laoId, challenge.toState(), federationResult.public_key));
        }
      } catch (e) {
        console.log(e);
        return false;
      }

      return true;
    }
    return false;
  };

/**
 * Handles an tokensExchange message.
 */
export const handleTokensExchangeMessage =
  (getCurrentLaoId: LinkedOrganizationsConfiguration['getCurrentLaoId']) =>
  (msg: ProcessableMessage) => {
    if (
      msg.messageData.object !== ObjectType.FEDERATION ||
      msg.messageData.action !== ActionType.TOKENS_EXCHANGE
    ) {
      console.warn('handleTokensExchangeMessage was called to process an unsupported message');
      return false;
    }
    const makeErr = (err: string) => `federation/tokensExchange was not processed: ${err}`;

    const laoId = getCurrentLaoId();
    if (!laoId) {
      console.warn(makeErr('no Lao is currently active'));
      return false;
    }

    if (msg.messageData instanceof TokensExchange) {
      const tokensExchange = msg.messageData as TokensExchange;
      if (
        tokensExchange.lao_id &&
        tokensExchange.roll_call_id &&
        tokensExchange.tokens &&
        tokensExchange.timestamp
      ) {
        dispatch(addLinkedLaoId(laoId, tokensExchange.lao_id));
        const subscribeChannels = async (): Promise<void> => {
          const subscribePromises = tokensExchange.tokens.map(async (attendee) => {
            try {
              await subscribeToChannel(
                tokensExchange.lao_id,
                dispatch,
                getUserSocialChannel(tokensExchange.lao_id, attendee),
              );
            } catch (err) {
              console.error(
                `Could not subscribe to social channel of attendee with public key '${attendee}', error:`,
                err,
              );
            }
          });

          try {
            await Promise.all(subscribePromises);
          } catch (err) {
            console.error('Error subscribing to one or more social channels:', err);
          }

          try {
            await subscribeToChannel(
              tokensExchange.lao_id,
              dispatch,
              getReactionChannel(tokensExchange.lao_id),
            );
          } catch (err) {
            console.error('Could not subscribe to reaction channel, error:', err);
          }
        };
        subscribeChannels();
        return true;
      }
    }
    return false;
  };
