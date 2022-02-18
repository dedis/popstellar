import { channelFromIds, EventTags, Hash, Timestamp } from 'model/objects';
import { OpenedLaoStore } from 'features/lao/store';
import { Lao } from 'features/lao/objects';
import { publish } from 'network/JsonRpcApi';

import { CastVote, EndElection, SetupElection } from './messages';
import { Question, Vote } from '../objects';

/**
 * Contains all functions to send election related messages.
 */

/**
 * Sends a query asking for the creation of an election.
 *
 * @param name - The name of the election
 * @param version - The version of the election
 * @param start- The start time of the election
 * @param end- The end time of the election
 * @param questions - The questions contained in the election
 * @param time - The creation time of the election
 */
export function requestCreateElection(
  name: string,
  version: string,
  start: Timestamp,
  end: Timestamp,
  questions: Question[],
  time: Timestamp,
): Promise<void> {
  const currentLao: Lao = OpenedLaoStore.get();

  const message = new SetupElection({
    lao: currentLao.id,
    id: Hash.fromStringArray(EventTags.ELECTION, currentLao.id.toString(), time.toString(), name),
    name: name,
    version: version,
    created_at: time,
    start_time: Timestamp.max(time, start),
    end_time: end,
    questions: questions,
  });

  const laoCh = channelFromIds(currentLao.id);
  return publish(laoCh, message);
}

/**
 * Sends a query to cast a vote during an election.
 *
 * @param election_id - The id of the ongoing election
 * @param votes - The votes to be added
 */
export function castVote(election_id: Hash, votes: Vote[]): Promise<void> {
  const time: Timestamp = Timestamp.EpochNow();
  const currentLao: Lao = OpenedLaoStore.get();
  const message = new CastVote({
    lao: currentLao.id,
    election: election_id,
    created_at: time,
    votes: votes,
  });

  const elecCh = channelFromIds(currentLao.id, election_id);
  return publish(elecCh, message);
}

/**
 * Sends a query to terminate an election.
 *
 * @param electionId - The id of the election
 * @param registeredVotes - The registered votes of the election
 */
export function terminateElection(electionId: Hash, registeredVotes: Hash): Promise<void> {
  const time: Timestamp = Timestamp.EpochNow();
  const currentLao: Lao = OpenedLaoStore.get();
  const message = new EndElection({
    lao: currentLao.id,
    election: electionId,
    created_at: time,
    registered_votes: registeredVotes,
  });

  const elecCh = channelFromIds(currentLao.id, electionId);
  return publish(elecCh, message);
}
