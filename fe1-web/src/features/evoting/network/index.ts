import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';

import { EvotingConfiguration } from '../objects';
import {
  handleCastVoteMessage,
  handleElectionEndMessage,
  handleElectionOpenMessage,
  handleElectionResultMessage,
  handleElectionSetupMessage,
} from './ElectionHandler';
import { CastVote, ElectionResult, EndElection, SetupElection } from './messages';
import { OpenElection } from './messages/OpenElection';

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
    handleElectionOpenMessage(config.getEventFromId, config.updateEvent),
    OpenElection.fromJson,
  );
  config.messageRegistry.add(
    ObjectType.ELECTION,
    ActionType.CAST_VOTE,
    handleCastVoteMessage(config.getCurrentLao, config.getEventFromId, config.updateEvent),
    CastVote.fromJson,
  );
  config.messageRegistry.add(
    ObjectType.ELECTION,
    ActionType.END,
    handleElectionEndMessage(config.getEventFromId, config.updateEvent),
    EndElection.fromJson,
  );
  config.messageRegistry.add(
    ObjectType.ELECTION,
    ActionType.RESULT,
    handleElectionResultMessage(config.getEventFromId, config.updateEvent),
    ElectionResult.fromJson,
  );
};
