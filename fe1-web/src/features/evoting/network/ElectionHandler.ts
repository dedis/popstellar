import { subscribeToChannel } from 'core/network';
import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { channelFromIds, getLastPartOfChannel, Hash } from 'core/objects';
import { dispatch } from 'core/redux';

import { EvotingConfiguration } from '../interface';
import { Election, ElectionStatus, ElectionVersion, RegisteredVote } from '../objects';
import { addElectionKey } from "../reducer";
import { CastVote, ElectionResult, EndElection, SetupElection } from './messages';
import { ElectionKey } from './messages/ElectionKey';
import { OpenElection } from './messages/OpenElection';

/**
 * Handlers for all election related messages coming from the network.
 */

/**
 * Returns a function that handles an ElectionKey message
 * It does so by storing the election key in the redux store
 */
export const handleElectionKeyMessage =
  (getLaoOrganizerBackendPublicKey: EvotingConfiguration['getLaoOrganizerBackendPublicKey']) =>
  (msg: ProcessableMessage) => {
    const makeErr = (err: string) => `election#key was not processed: ${err}`;

    if (
      msg.messageData.object !== ObjectType.ELECTION ||
      msg.messageData.action !== ActionType.KEY
    ) {
      console.warn(
        makeErr(
          `Invalid object or action parameter: ${msg.messageData.object}#${msg.messageData.action}`,
        ),
      );
      return false;
    }

    // obtain the lao id from the channel
    if (!msg.laoId) {
      console.warn(makeErr('message was not sent on a lao subchannel'));
      return false;
    }

    const electionKeyMessage = msg.messageData as ElectionKey;
    // for now *ALL* election#key messages *MUST* be sent by the backend of the organizer
    const organizerBackendPublicKey = getLaoOrganizerBackendPublicKey(msg.laoId.valueOf());

    if (!organizerBackendPublicKey) {
      console.warn(makeErr("the organizer backend's public key is unknown"));
      return false;
    }

    if (organizerBackendPublicKey.valueOf() !== msg.sender.valueOf()) {
      console.warn(makeErr("the senders' public key does not match the organizer backend's"));
      return false;
    }

    dispatch(
      addElectionKey({
        electionId: electionKeyMessage.election.valueOf(),
        electionKey: electionKeyMessage.election_key.valueOf(),
      }),
    );

    return true;
  };

/**
 * Returns a function that handles an ElectionSetup message by setting up the election in the current Lao.
 * @param addElection - A function to add a new election
 */
export const handleElectionSetupMessage =
  (addElection: (laoId: Hash | string, election: Election) => void) =>
  (msg: ProcessableMessage): boolean => {
    const makeErr = (err: string) => `election#setup was not processed: ${err}`;

    if (
      msg.messageData.object !== ObjectType.ELECTION ||
      msg.messageData.action !== ActionType.SETUP
    ) {
      console.warn(
        makeErr(
          `Invalid object or action parameter: ${msg.messageData.object}#${msg.messageData.action}`,
        ),
      );
      return false;
    }

    if (!msg.laoId) {
      console.warn(makeErr(`Was not sent on a lao subchannel but rather on '${msg.channel}'`));
      return false;
    }

    const elecMsg = msg.messageData as SetupElection;

    // check if the election version is supported by the frontend
    if (
      ![ElectionVersion.OPEN_BALLOT, ElectionVersion.SECRET_BALLOT].includes(
        elecMsg.version as ElectionVersion,
      )
    ) {
      console.warn(
        'handleElectionSetupMessage was called to process an unsupported election version',
        msg,
      );
      return false;
    }

    const election = new Election({
      lao: elecMsg.lao,
      id: elecMsg.id,
      name: elecMsg.name,
      version: elecMsg.version as ElectionVersion,
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

    addElection(msg.laoId, election);
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
    updateElection: (election: Election) => void,
  ) =>
  (msg: ProcessableMessage): boolean => {
    console.log('Handling Election open message');

    const makeErr = (err: string) => `election#open was not processed: ${err}`;

    if (
      msg.messageData.object !== ObjectType.ELECTION ||
      msg.messageData.action !== ActionType.OPEN
    ) {
      console.warn(
        makeErr(
          `Invalid object or action parameter: ${msg.messageData.object}#${msg.messageData.action}`,
        ),
      );
      return false;
    }

    if (!msg.laoId) {
      console.warn(makeErr(`Was not sent on a lao subchannel but rather on '${msg.channel}'`));
      return false;
    }

    const electionOpenMsg = msg.messageData as OpenElection;
    const election = getElectionById(electionOpenMsg.election) as Election;
    if (!election) {
      console.warn(makeErr('No active election to end'));
      return false;
    }

    // Change election status here such that it will change the election display in the event list
    election.electionStatus = ElectionStatus.OPENED;
    updateElection(election);
    return true;
  };

/**
 * Returns a function that handles a CastVote message being sent during an election.
 * @param getElectionById - A function get an election by its id
 * @param updateElection - A function to update an election
 */
export const handleCastVoteMessage =
  (
    getElectionById: (electionId: Hash | string) => Election | undefined,
    updateElection: (election: Election) => void,
  ) =>
  (msg: ProcessableMessage): boolean => {
    const makeErr = (err: string) => `election#cast-vote was not processed: ${err}`;

    if (
      msg.messageData.object !== ObjectType.ELECTION ||
      msg.messageData.action !== ActionType.CAST_VOTE
    ) {
      console.warn(
        makeErr(
          `Invalid object or action parameter: ${msg.messageData.object}#${msg.messageData.action}`,
        ),
      );
      return false;
    }

    if (!msg.laoId) {
      console.warn(makeErr(`Was not sent on a lao subchannel but rather on '${msg.channel}'`));
      return false;
    }

    const castVoteMsg = msg.messageData as CastVote;

    const election = getElectionById(castVoteMsg.election) as Election;
    if (!election) {
      console.warn(makeErr('No active election to register vote'));
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

    updateElection(election);
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
    updateElection: (election: Election) => void,
  ) =>
  (msg: ProcessableMessage) => {
    console.log('Handling Election end message');

    const makeErr = (err: string) => `election#end was not processed: ${err}`;

    if (
      msg.messageData.object !== ObjectType.ELECTION ||
      msg.messageData.action !== ActionType.END
    ) {
      console.warn(
        makeErr(
          `Invalid object or action parameter: ${msg.messageData.object}#${msg.messageData.action}`,
        ),
      );
      return false;
    }

    if (!msg.laoId) {
      console.warn(makeErr(`Was not sent on a lao subchannel but rather on '${msg.channel}'`));
      return false;
    }

    const ElectionEndMsg = msg.messageData as EndElection;
    const election = getElectionById(ElectionEndMsg.election) as Election;
    if (!election) {
      console.warn(makeErr('No active election to end'));
      return false;
    }

    // Change election status here such that it will change the election display in the event list
    election.electionStatus = ElectionStatus.TERMINATED;
    updateElection(election);
    return true;
  };

/**
 * Returns a function that handles an ElectionResult message by updating the election's state with its results.
 * @param getElectionById - A function get an election by its id
 * @param updateElection - A function to update an election
 * @param getLaoOrganizerBackendPublicKey - A function returning the public key of the lao organizer's backend
 */
export const handleElectionResultMessage =
  (
    getElectionById: (electionId: Hash | string) => Election | undefined,
    updateElection: (election: Election) => void,
    getLaoOrganizerBackendPublicKey: EvotingConfiguration['getLaoOrganizerBackendPublicKey'],
  ) =>
  (msg: ProcessableMessage) => {
    const makeErr = (err: string) => `election#result was not processed: ${err}`;

    if (
      msg.messageData.object !== ObjectType.ELECTION ||
      msg.messageData.action !== ActionType.RESULT
    ) {
      console.warn(
        makeErr(
          `Invalid object or action parameter: ${msg.messageData.object}#${msg.messageData.action}`,
        ),
      );
      return false;
    }

    if (!msg.laoId) {
      console.warn(makeErr(`Was not sent on a lao subchannel but rather on '${msg.channel}'`));
      return false;
    }

    if (!msg.channel) {
      console.warn(makeErr('No channel found in message'));
      return false;
    }

    // for now *ALL* election#result messages *MUST* be sent by the backend of the organizer
    const organizerBackendPublicKey = getLaoOrganizerBackendPublicKey(msg.laoId.valueOf());

    if (!organizerBackendPublicKey) {
      console.warn(makeErr("the organizer backend's public key is unknown"));
      return false;
    }

    if (organizerBackendPublicKey.valueOf() !== msg.sender.valueOf()) {
      console.warn(makeErr("the senders' public key does not match the organizer backend's"));
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
    updateElection(election);
    return true;
  };
