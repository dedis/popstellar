import { Message } from 'model/network/method/message';
import {
  ActionType, ObjectType, SetupElection, CastVote,
} from 'model/network/method/message/data';
import { channelFromIds, Election, RegisteredVote } from 'model/objects';
import {
  addEvent, updateEvent, dispatch, getStore, makeCurrentLao, KeyPairStore,
} from 'store';
import { subscribeToChannel } from 'network/CommunicationApi';
import { getEventFromId } from './Utils';

const getCurrentLao = makeCurrentLao();

function handleElectionSetupMessage(msg: Message): boolean {
  if (msg.messageData.object !== ObjectType.ELECTION
    || msg.messageData.action !== ActionType.SETUP) {
    console.warn('handleElectionSetupMessage was called to process an unsupported message', msg);
    return false;
  }

  const makeErr = (err: string) => `election/setup was not processed: ${err}`;

  const storeState = getStore().getState();
  const lao = getCurrentLao(storeState);
  if (!lao) {
    console.warn(makeErr('no LAO is currently active'));
    return false;
  }

  const elecMsg = msg.messageData as SetupElection;

  const election = new Election({
    lao: elecMsg.lao,
    id: elecMsg.id,
    name: elecMsg.name,
    version: elecMsg.version,
    created_at: elecMsg.created_at,
    start: elecMsg.start_time,
    end: elecMsg.end_time,
    questions: elecMsg.questions,
    registered_votes: [],
  });

  // Subscribing to the election channel corresponding to that election
  const electionChannel = channelFromIds(election.lao, election.id);
  subscribeToChannel(electionChannel).then(() => {}).catch((err) => {
    console.error('Could not subscribe to Election channel, error:', err);
  });

  dispatch(addEvent(lao.id, election.toState()));
  return true;
}

function handleCastVoteMessage(msg: Message): boolean {
  if (msg.messageData.object !== ObjectType.ELECTION
    || msg.messageData.action !== ActionType.CAST_VOTE) {
    console.warn('handleCastVoteMessage was called to process an unsupported message', msg);
    return false;
  }
  const makeErr = (err: string) => `election/cast-vote was not processed: ${err}`;
  const storeState = getStore().getState();
  const lao = getCurrentLao(storeState);
  if (!lao) {
    console.warn(makeErr('no LAO is currently active'));
    return false;
  }
  const myPublicKey = KeyPairStore.getPublicKey();
  const isOrganizer = lao.organizer.equals(myPublicKey);
  const isWitness = lao.witnesses.some((w) => w.equals(myPublicKey));
  if (!isOrganizer && !isWitness) {
    // Then current user is an attendee and doesn't have to store the votes
    return true;
  }

  const castVoteMsg = msg.messageData as CastVote;
  const currentVote: RegisteredVote = {
    createdAt: castVoteMsg.created_at,
    sender: msg.sender,
    votes: castVoteMsg.votes,
    messageId: msg.message_id,
  };
  const election = getEventFromId(storeState, castVoteMsg.election) as Election;
  if (!election) {
    console.warn(makeErr('No active election to register vote '));
    return true;
  }

  if (election.registered_votes.some(
    (votes) => votes.sender.toString() === currentVote.sender.toString(),
  )) { // Update the vote if the person has already voted before
    election.registered_votes = election.registered_votes.map(
      (prevVote) => (
        prevVote.sender.toString() === currentVote.sender.toString()
        && prevVote.createdAt.valueOf() < currentVote.createdAt.valueOf() ? currentVote : prevVote),
    );
  } else {
    election.registered_votes = [...election.registered_votes, currentVote];
  }
  dispatch(updateEvent(lao.id, election.toState()));
  return true;
}

export function handleElectionMessage(msg: Message) {
  if (msg.messageData.object !== ObjectType.ELECTION) {
    console.warn('handleElectionMessage was called to process an unsupported message', msg);
    return false;
  }

  switch (msg.messageData.action) {
    case ActionType.SETUP:
      return handleElectionSetupMessage(msg);
    case ActionType.CAST_VOTE:
      return handleCastVoteMessage(msg);

    default:
      console.warn('A Election message was received but'
        + ' its processing logic is not yet implemented:', msg);
      return false;
  }
}
