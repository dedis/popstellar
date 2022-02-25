import { channelFromIds, EventTags, Hash, Timestamp } from 'core/objects';
import { OpenedLaoStore } from 'features/lao/store';
import { Lao } from 'features/lao/objects';
import { publish } from 'core/network';

import { CastVote, EndElection, SetupElection } from './messages';
import { Election, Question, Vote } from '../objects';

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

  // publish on the general LAO channel
  return publish(channelFromIds(currentLao.id), message);
}

/**
 * Sends a query to cast a vote during an election.
 *
 * @param election - The election for which the vote should be casted
 * @param votes - The votes to be added, a map from question index to the set of selected answer indices
 */
export function castVote(
  election: Election,
  votes: { [questionIndex: number]: Set<number> },
): Promise<void> {
  const time: Timestamp = Timestamp.EpochNow();
  const currentLao: Lao = OpenedLaoStore.get();
  const message = new CastVote({
    lao: currentLao.id,
    election: election.id,
    created_at: time,
    // Convert object to array
    votes: Object.entries(votes)
      .map(([idx, selectionOptions]) => ({
        index: parseInt(idx, 10),
        selectionOptions,
      }))
      // sort it by index
      .sort((a, b) => a.index - b.index)
      // and add an id to all votes as well as the matching question id
      .map<Vote>(({ index, selectionOptions }) => ({
        id: CastVote.computeVoteId(election, index, selectionOptions).valueOf(),
        question: election.questions[index].id,
        // sort votes in ascending order
        vote: [...selectionOptions].sort(),
      })),
  });
  console.log(message.votes);

  // publish on the LAO channel specific to this election
  return publish(channelFromIds(currentLao.id, election.id), message);
}

/**
 * Sends a query to terminate an election.
 *
 * @param election - The election that should be terminated
 */
export function terminateElection(election: Election): Promise<void> {
  const time: Timestamp = Timestamp.EpochNow();
  const currentLao: Lao = OpenedLaoStore.get();
  const message = new EndElection({
    lao: currentLao.id,
    election: election.id,
    created_at: time,
    registered_votes: EndElection.computeRegisteredVotesHash(election),
  });

  // publish on the LAO channel specific to this election
  return publish(channelFromIds(currentLao.id, election.id), message);
}
