import { ActionType, MessageRegistry, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, PublicKey } from 'core/objects';
import { dispatch } from 'core/redux';
import { LinkedOrganizationsCompositionConfiguration } from '../interface';
import { handleChallengeMessage, handleRequestChallengeMessage } from './LinkedOrgHandler';
import { ChallengeMessage } from './messages/ChallengeMessage';
import { RequestChallenge } from './messages';

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
    handleChallengeMessage(configuration.getLaoOrganizerBackendPublicKey),
    ChallengeMessage.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.FEDERATION,
    ActionType.CHALLENGE_REQUEST,
    handleRequestChallengeMessage(configuration.getCurrentLaoId),
    RequestChallenge.fromJson,
  );
}
