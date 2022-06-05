import { publish } from 'core/network';
import { channelFromIds, EventTags, Hash, Timestamp } from 'core/objects';

import { Election, Question, SelectedBallots } from '../objects';
import { CastVote, EndElection, SetupElection } from './messages';
import { OpenElection } from './messages/OpenElection';

/**
 * Contains all functions to send election related messages.
 */

/**
 * Sends a query asking for the creation of an election.
 *
 * @param laoId - The id of the lao where this election should be created in
 * @param name - The name of the election
 * @param version - The version of the election
 * @param start - The start time of the election
 * @param end - The end time of the election
 * @param questions - The questions contained in the election
 * @param time - The creation time of the election
 */
export function requestCreateElection(
  laoId: Hash,
  name: string,
  version: string,
  start: Timestamp,
  end: Timestamp,
  questions: Question[],
  time: Timestamp,
): Promise<void> {
  const message = new SetupElection(
    {
      lao: laoId,
      id: Hash.fromStringArray(EventTags.ELECTION, laoId.valueOf(), time.toString(), name),
      name: name,
      version: version,
      created_at: time,
      start_time: Timestamp.max(time, start),
      end_time: end,
      questions: questions,
    },
    laoId,
  );

  // publish on the general LAO channel
  return publish(channelFromIds(laoId), message);
}

/**
 * Sends a query to open an election.
 *
 * @param laoId - The id of the lao in which the given election should be opened
 * @param election - The election that should be opened
 */
export function openElection(laoId: Hash, election: Election): Promise<void> {
  const time: Timestamp = Timestamp.EpochNow();
  const message = new OpenElection({
    lao: laoId,
    election: election.id,
    opened_at: time,
  });

  // publish on the LAO channel specific to this election
  return publish(channelFromIds(laoId, election.id), message);
}

/**
 * Sends a query to cast a vote during an election.
 *
 * @param laoId - The id of the lao in which this vote should be casted
 * @param election - The election for which the vote should be casted
 * @param selectedBallots - The votes to be added, a map from question index to the set of selected answer indices
 */
export function castVote(
  laoId: Hash,
  election: Election,
  selectedBallots: SelectedBallots,
): Promise<void> {
  const time: Timestamp = Timestamp.EpochNow();

  const message = new CastVote({
    lao: laoId,
    election: election.id,
    created_at: time,
    // Convert object to array
    votes: CastVote.selectedBallotsToVotes(election, selectedBallots),
  });

  // publish on the LAO channel specific to this election
  return publish(channelFromIds(laoId, election.id), message);
}

/**
 * Sends a query to terminate an election.
 *
 * @param laoId - The id of the lao in which the given election should be terminated
 * @param election - The election that should be terminated
 */
export function terminateElection(laoId: Hash, election: Election): Promise<void> {
  const time: Timestamp = Timestamp.EpochNow();
  const message = new EndElection({
    lao: laoId,
    election: election.id,
    created_at: time,
    registered_votes: EndElection.computeRegisteredVotesHash(election),
  });

  // publish on the LAO channel specific to this election
  return publish(channelFromIds(laoId, election.id), message);
}
