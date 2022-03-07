import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { CastVote, ElectionResult, EndElection, SetupElection } from './messages';
import {
  handleElectionSetupMessage,
  handleCastVoteMessage,
  handleElectionEndMessage,
  handleElectionResultMessage,
  handleElectionOpenMessage,
} from './ElectionHandler';
import { OpenElection } from './messages/OpenElection';
import { EvotingConfiguration } from '../objects';

/**
 * Configures the network callbacks in a MessageRegistry.
 * @param getCurrentLao - A function returning the current load
 * @param getCurrentLaoId - A function returning the current loa id
 * @param getEventFromId - A function retrieving an event with matching id from the store of the currently active lao
 * @param addEvent - A function creating a redux action to add a new event to the store of the currently active lao
 * @param updateEvent - A function returning a redux action for update an event in the currently active lao store
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export const configureNetwork = (
  getCurrentLao: EvotingConfiguration['getCurrentLao'],
  getCurrentLaoId: EvotingConfiguration['getCurrentLaoId'],
  getEventFromId: EvotingConfiguration['getEventFromId'],
  addEvent: EvotingConfiguration['addEvent'],
  updateEvent: EvotingConfiguration['updateEvent'],
  registry: EvotingConfiguration['messageRegistry'],
) => {
  registry.add(
    ObjectType.ELECTION,
    ActionType.SETUP,
    handleElectionSetupMessage(addEvent),
    SetupElection.fromJson,
  );
  registry.add(
    ObjectType.ELECTION,
    ActionType.OPEN,
    handleElectionOpenMessage(getEventFromId, updateEvent),
    OpenElection.fromJson,
  );
  registry.add(
    ObjectType.ELECTION,
    ActionType.CAST_VOTE,
    handleCastVoteMessage(getCurrentLao, getEventFromId, updateEvent),
    CastVote.fromJson,
  );
  registry.add(
    ObjectType.ELECTION,
    ActionType.END,
    handleElectionEndMessage(getEventFromId, updateEvent),
    EndElection.fromJson,
  );
  registry.add(
    ObjectType.ELECTION,
    ActionType.RESULT,
    handleElectionResultMessage(getEventFromId, updateEvent),
    ElectionResult.fromJson,
  );
};
