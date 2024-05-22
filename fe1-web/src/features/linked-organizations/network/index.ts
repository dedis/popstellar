import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';

import { LinkedOrganizationsConfiguration } from '../interface';
import {
  handleChallengeMessage,
  handleChallengeRequestMessage,
  handleFederationExpectMessage,
  handleFederationInitMessage,
} from './LinkedOrgHandler';
import { ChallengeRequest } from './messages';
import { ChallengeMessage } from './messages/ChallengeMessage';
import { FederationExpect } from './messages/FederationExpect';
import { FederationInit } from './messages/FederationInit';

export * from './LinkedOrgMessageApi';

/**
 * Configures the network callbacks in a MessageRegistry.
 *
 *  @param configuration - The configuration object for the linked organizationfeature.
 */
export function configureNetwork(configuration: LinkedOrganizationsConfiguration) {
  configuration.messageRegistry.add(
    ObjectType.FEDERATION,
    ActionType.CHALLENGE,
    handleChallengeMessage(),
    ChallengeMessage.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.FEDERATION,
    ActionType.CHALLENGE_REQUEST,
    handleChallengeRequestMessage(configuration.getCurrentLaoId),
    ChallengeRequest.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.FEDERATION,
    ActionType.FEDERATION_INIT,
    handleFederationInitMessage(configuration.getCurrentLaoId),
    FederationInit.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.FEDERATION,
    ActionType.FEDERATION_EXPECT,
    handleFederationExpectMessage(configuration.getCurrentLaoId),
    FederationExpect.fromJson,
  );
}
