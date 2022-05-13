import { KeyPairStore } from 'core/keypair';
import { subscribeToChannel } from 'core/network';
import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { channelFromIds, getLastPartOfChannel, Hash } from 'core/objects';

import { EvotingConfiguration } from '../interface';
import { Election, ElectionState, ElectionStatus, RegisteredVote } from '../objects';
import { CastVote, ElectionResult, EndElection, SetupElection } from './messages';
import { OpenElection } from './messages/OpenElection';

/**
 * Handlers for all election related messages coming from the network.
 */

/**
 * Returns a function that handles an ElectionSetup message by setting up the election in the current Lao.
 * @param addElection - A function to add a new election
 */
export const handleElectionSetupMessage =
  (addElection: (laoId: Hash | string, electionState: ElectionState) => void) =>
  (msg: ProcessableMessage): boolean => {
    if (
      msg.messageData.object !== ObjectType.ELECTION ||
      msg.messageData.action !== ActionType.SETUP ||
      !msg.laoId
    ) {
      console.warn('handleElectionSetupMessage was called to process an unsupported message', msg);
      return false;
    }

    const elecMsg = msg.messageData as SetupElection;

    const election = new Election({
      lao: elecMsg.lao,
      id: elecMsg.id,
      name: elecMsg.name,
      version: elecMsg.version,
      createdAt: elecMsg.created_at,
      start: elecMsg.start_time,
      end: elecMsg.end_time,
      questions: elecMsg.questions,
      electionStatus: ElectionStatus.NOT_STARTED,
      registeredVotes: [],
    });

    // Subscribing to the election channel corresponding to that election
    const electionChannel = channelFromIds(election.lao, election.id);
    subscribeToChannel(electionChannel).catch((err) => {
      console.error('Could not subscribe to Election channel, error:', err);
    });

    addElection(msg.laoId, election.toState());
    return true;
  };

/**
 * Returns a function that handles an ElectionOpen message by opening the election.
 * @param getElectionById - A function get an election by its id
 * @param updateElection - A function to update an election
 */
export const handleElectionOpenMessage =
  (
    getElectionById: (electionId: Hash | string) => Election | undefined,
    updateElection: (laoId: Hash | string, electionState: ElectionState) => void,
  ) =>
  (msg: ProcessableMessage): boolean => {
    console.log('Handling Election open message');
    if (
      msg.messageData.object !== ObjectType.ELECTION ||
      msg.messageData.action !== ActionType.OPEN ||
      !msg.laoId
    ) {
      console.warn('handleElectionOpenMessage was called to process an unsupported message', msg);
      return false;
    }
    const makeErr = (err: string) => `election/open was not processed: ${err}`;

    const electionOpenMsg = msg.messageData as OpenElection;
    const election = getElectionById(electionOpenMsg.election) as Election;
    if (!election) {
      console.warn(makeErr('No active election to end'));
      return false;
    }

    // Change election status here such that it will change the election display in the event list
    election.electionStatus = ElectionStatus.OPENED;
    updateElection(msg.laoId, election.toState());
    return true;
  };

/**
 * Returns a function that handles a CastVote message being sent during an election.
 * @param getCurrentLao - A function returning the current lao
 * @param getElectionById - A function get an election by its id
 * @param updateElection - A function to update an election
 */
export const handleCastVoteMessage =
  (
    getCurrentLao: EvotingConfiguration['getCurrentLao'],
    getElectionById: (electionId: Hash | string) => Election | undefined,
    updateElection: (laoId: Hash | string, electionState: ElectionState) => void,
  ) =>
  (msg: ProcessableMessage): boolean => {
    if (
      msg.messageData.object !== ObjectType.ELECTION ||
      msg.messageData.action !== ActionType.CAST_VOTE ||
      !msg.laoId
    ) {
      console.warn('handleCastVoteMessage was called to process an unsupported message', msg);
      return false;
    }
    const makeErr = (err: string) => `election/cast-vote was not processed: ${err}`;
    const lao = getCurrentLao();

    const myPublicKey = KeyPairStore.getPublicKey();
    const isOrganizer = lao.organizer.equals(myPublicKey);
    const isWitness = lao.witnesses.some((w) => w.equals(myPublicKey));
    if (!isOrganizer && !isWitness) {
      // Then current user is an attendee and doesn't have to store the votes
      return true;
    }

    const castVoteMsg = msg.messageData as CastVote;

    const election = getElectionById(castVoteMsg.election) as Election;
    if (!election) {
      console.warn(makeErr('No active election to register vote '));
      return false;
    }

    const currentVote: RegisteredVote = {
      createdAt: castVoteMsg.created_at.valueOf(),
      sender: msg.sender.valueOf(),
      votes: castVoteMsg.votes,
      messageId: msg.message_id.valueOf(),
    };

    if (election.registeredVotes.some((votes) => votes.sender === currentVote.sender)) {
      // Update the vote if the person has already voted before
      election.registeredVotes = election.registeredVotes.map((prevVote) =>
        prevVote.sender === currentVote.sender && prevVote.createdAt < currentVote.createdAt
          ? currentVote
          : prevVote,
      );
    } else {
      election.registeredVotes = [...election.registeredVotes, currentVote];
    }
    updateElection(msg.laoId, election.toState());
    return true;
  };

/**
 * Returns a function that handles an ElectionEnd message by ending the election.
 * @param getElectionById - A function get an election by its id
 * @param updateElection - A function to update an election
 */
export const handleElectionEndMessage =
  (
    getElectionById: (electionId: Hash | string) => Election | undefined,
    updateElection: (laoId: Hash | string, electionState: ElectionState) => void,
  ) =>
  (msg: ProcessableMessage) => {
    console.log('Handling Election end message');
    if (
      msg.messageData.object !== ObjectType.ELECTION ||
      msg.messageData.action !== ActionType.END ||
      !msg.laoId
    ) {
      console.warn('handleElectionEndMessage was called to process an unsupported message', msg);
      return false;
    }
    const makeErr = (err: string) => `election/end was not processed: ${err}`;

    const ElectionEndMsg = msg.messageData as EndElection;
    const election = getElectionById(ElectionEndMsg.election) as Election;
    if (!election) {
      console.warn(makeErr('No active election to end'));
      return false;
    }

    // Change election status here such that it will change the election display in the event list
    election.electionStatus = ElectionStatus.TERMINATED;
    updateElection(msg.laoId, election.toState());
    return true;
  };

/**
 * Returns a function that handles an ElectionResult message by updating the election's state with its results.
 * @param getElectionById - A function get an election by its id
 * @param updateElection - A function to update an election
 */
export const handleElectionResultMessage =
  (
    getElectionById: (electionId: Hash | string) => Election | undefined,
    updateElection: (laoId: Hash | string, electionState: ElectionState) => void,
  ) =>
  (msg: ProcessableMessage) => {
    if (
      msg.messageData.object !== ObjectType.ELECTION ||
      msg.messageData.action !== ActionType.RESULT ||
      !msg.laoId
    ) {
      console.warn('handleElectionResultMessage was called to process an unsupported message', msg);
      return false;
    }

    const makeErr = (err: string) => `election/Result was not processed: ${err}`;

    if (!msg.channel) {
      console.warn(makeErr('No channel found in message'));
      return false;
    }
    const electionId = getLastPartOfChannel(msg.channel);
    const electionResultMessage = msg.messageData as ElectionResult;
    const election = getElectionById(electionId) as Election;
    if (!election) {
      console.warn(makeErr('No active election for the result'));
      return false;
    }

    election.questionResult = electionResultMessage.questions.map((q) => ({
      id: q.id,
      result: q.result.map((r) => ({ ballotOption: r.ballot_option, count: r.count })),
    }));

    election.electionStatus = ElectionStatus.RESULT;
    updateElection(msg.laoId, election.toState());
    return true;
  };
