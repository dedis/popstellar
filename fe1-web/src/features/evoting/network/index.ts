import { MessageRegistry, ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { CastVote, ElectionResult, EndElection, SetupElection } from './messages';
import {
  handleElectionSetupMessage,
  handleCastVoteMessage,
  handleElectionEndMessage,
  handleElectionResultMessage,
} from './ElectionHandler';

/**
 * Configures the network callbacks in a MessageRegistry.
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configureNetwork(registry: MessageRegistry) {
  registry.add(
    ObjectType.ELECTION,
    ActionType.SETUP,
    handleElectionSetupMessage,
    SetupElection.fromJson,
  );
  registry.add(ObjectType.ELECTION, ActionType.CAST_VOTE, handleCastVoteMessage, CastVote.fromJson);
  registry.add(ObjectType.ELECTION, ActionType.END, handleElectionEndMessage, EndElection.fromJson);
  registry.add(
    ObjectType.ELECTION,
    ActionType.RESULT,
    handleElectionResultMessage,
    ElectionResult.fromJson,
  );
}
