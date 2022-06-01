import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash } from 'core/objects';
import { dispatch, getStore } from 'core/redux';

import { EvotingConfiguration } from '../interface';
import { Election } from '../objects';
import { addElection, getElectionById, updateElection } from '../reducer';
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
 * @param configuration - An evoting config object
 */
export const configureNetwork = (configuration: EvotingConfiguration) => {
  // getElectionById bound to the global state
  const boundGetElectionById = (electionId: Hash | string) =>
    getElectionById(electionId, getStore().getState());

  const addElectionEvent = (laoId: Hash | string, election: Election) => {
    const electionState = election.toState();

    dispatch(addElection(electionState));
    dispatch(
      configuration.addEvent(laoId, {
        eventType: Election.EVENT_TYPE,
        id: electionState.id,
        start: electionState.start,
        end: electionState.start,
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
        start: electionState.start,
        end: electionState.start,
      }),
    );
  };

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
    handleElectionResultMessage(boundGetElectionById, updateElectionEvent),
    ElectionResult.fromJson,
  );
};
