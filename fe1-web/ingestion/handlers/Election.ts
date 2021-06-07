import { Message } from 'model/network/method/message';
import { ActionType, ObjectType, SetupElection } from 'model/network/method/message/data';
import { channelFromIds, Election } from 'model/objects';
import {
  addEvent, dispatch, getStore, makeCurrentLao,
} from 'store';
import { subscribeToChannel } from 'network/CommunicationApi';

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
  });

  dispatch(addEvent(lao.id, election.toState()));
  // Subscribing to the election channel corresponding to that election
  let isSubscribed = false;
  const electionChannel = channelFromIds(election.lao, election.id);
  subscribeToChannel(electionChannel).then(() => { isSubscribed = true; }).catch((err) => {
    console.error('Could not subscribe to Election channel, error:', err);
    isSubscribed = false;
  });
  return isSubscribed;
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
