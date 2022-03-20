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
import { EvotingConfiguration } from '../interface';

/**
 * Configures the network callbacks in a MessageRegistry.
 * @param config - An evoting config object
 */
export const configureNetwork = (config: EvotingConfiguration) => {
  config.messageRegistry.add(
    ObjectType.ELECTION,
    ActionType.SETUP,
    handleElectionSetupMessage(config.addEvent),
    SetupElection.fromJson,
  );
  config.messageRegistry.add(
    ObjectType.ELECTION,
    ActionType.OPEN,
    handleElectionOpenMessage(config.getEventById, config.updateEvent),
    OpenElection.fromJson,
  );
  config.messageRegistry.add(
    ObjectType.ELECTION,
    ActionType.CAST_VOTE,
    handleCastVoteMessage(config.getCurrentLao, config.getEventById, config.updateEvent),
    CastVote.fromJson,
  );
  config.messageRegistry.add(
    ObjectType.ELECTION,
    ActionType.END,
    handleElectionEndMessage(config.getEventById, config.updateEvent),
    EndElection.fromJson,
  );
  config.messageRegistry.add(
    ObjectType.ELECTION,
    ActionType.RESULT,
    handleElectionResultMessage(config.getEventById, config.updateEvent),
    ElectionResult.fromJson,
  );
};
