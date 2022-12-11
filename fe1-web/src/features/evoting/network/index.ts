import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash } from 'core/objects';
import { dispatch, getStore } from 'core/redux';

import { EvotingConfiguration } from '../interface';
import { Election, ElectionStatus } from '../objects';
import { addElection, getElectionById, updateElection } from '../reducer';
import {
  handleCastVoteMessage,
  handleElectionEndMessage,
  handleElectionKeyMessage,
  handleElectionOpenMessage,
  handleElectionResultMessage,
  handleElectionSetupMessage,
} from './ElectionHandler';
import { CastVote, ElectionResult, EndElection, SetupElection } from './messages';
import { ElectionKey } from './messages/ElectionKey';
import { OpenElection } from './messages/OpenElection';

/**
 * Configures the network callbacks in a MessageRegistry.
 * @param configuration - An evoting config object
 */
export const configureNetwork = (configuration: EvotingConfiguration) => {
  // getElectionById bound to the global state
  const boundGetElectionById = (electionId: Hash) =>
    getElectionById(electionId, getStore().getState());

  const addElectionEvent = (laoId: Hash, election: Election) => {
    const electionState = election.toState();

    dispatch(addElection(electionState));
    dispatch(
      configuration.addEvent(laoId, {
        eventType: Election.EVENT_TYPE,
        id: electionState.id,
        start: election.start.valueOf(),
        end:
          election.electionStatus === ElectionStatus.RESULT ||
          election.electionStatus === ElectionStatus.TERMINATED
            ? election.end.valueOf()
            : undefined,
      }),
    );
  };

  const updateElectionEvent = (election: Election) => {
    const electionState = election.toState();

    dispatch(updateElection(electionState));
    dispatch(
      configuration.updateEvent({
        eventType: Election.EVENT_TYPE,
        id: electionState.id,
        start: election.start.valueOf(),
        end:
          election.electionStatus === ElectionStatus.RESULT ||
          election.electionStatus === ElectionStatus.TERMINATED
            ? election.end.valueOf()
            : undefined,
      }),
    );
  };

  configuration.messageRegistry.add(
    ObjectType.ELECTION,
    ActionType.KEY,
    handleElectionKeyMessage(configuration.getLaoOrganizerBackendPublicKey),
    ElectionKey.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.ELECTION,
    ActionType.SETUP,
    handleElectionSetupMessage(addElectionEvent),
    SetupElection.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.ELECTION,
    ActionType.OPEN,
    handleElectionOpenMessage(boundGetElectionById, updateElectionEvent),
    OpenElection.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.ELECTION,
    ActionType.CAST_VOTE,
    handleCastVoteMessage(boundGetElectionById, updateElectionEvent),
    CastVote.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.ELECTION,
    ActionType.END,
    handleElectionEndMessage(boundGetElectionById, updateElectionEvent),
    EndElection.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.ELECTION,
    ActionType.RESULT,
    handleElectionResultMessage(
      boundGetElectionById,
      updateElectionEvent,
      configuration.getLaoOrganizerBackendPublicKey,
    ),
    ElectionResult.fromJson,
  );
};
