import { ActionType, MessageRegistry, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, PublicKey } from 'core/objects';
import { dispatch } from 'core/redux';
import { LinkedOrganizationsCompositionConfiguration } from '../interface';
import { handleChallengeMessage, handleChallengeRequestMessage, handleFederationExpectMessage, handleFederationInitMessage } from './LinkedOrgHandler';
import { ChallengeMessage } from './messages/ChallengeMessage';
import { ChallengeRequest } from './messages';
import { FederationInit } from './messages/FederationInit';
import { FederationExpect } from './messages/FederationExpect';
export * from './LinkedOrgMessageApi';

/**
 * Configures the network callbacks in a MessageRegistry.
 *
 *  @param configuration - The configuration object for the linked organizationfeature.
 */
export function configureNetwork(configuration: LinkedOrganizationsCompositionConfiguration
) {
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
