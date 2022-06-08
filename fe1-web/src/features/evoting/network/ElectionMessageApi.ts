import { publish } from 'core/network';
import { channelFromIds, EventTags, Hash, Timestamp } from 'core/objects';

import {
  Election,
  ElectionVersion,
  EncryptedVote,
  Question,
  SelectedBallots,
  Vote,
} from '../objects';
import { ElectionPublicKey } from '../objects/ElectionPublicKey';
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
export async function requestCreateElection(
  laoId: Hash,
  name: string,
  version: ElectionVersion,
  start: Timestamp,
  end: Timestamp,
  questions: Question[],
  time: Timestamp,
): Promise<void> {
  const message = new SetupElection(
    {
      version: version,
      id: Hash.fromStringArray(EventTags.ELECTION, laoId.valueOf(), time.toString(), name),
      lao: laoId,
      name: name,
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
 * @param election - The election that should be opened
 */
export function openElection(election: Election): Promise<void> {
  const time: Timestamp = Timestamp.EpochNow();
  const message = new OpenElection({
    lao: election.lao,
    election: election.id,
    opened_at: time,
  });

  // publish on the LAO channel specific to this election
  return publish(channelFromIds(election.lao, election.id), message);
}

/**
 * Sends a query to cast a vote during an election.
 *
 * @param election - The election for which the vote should be casted
 * @param electionKey - The public key of the election or undefined if there is none
 * @param selectedBallots - The votes to be added, a map from question index to the set of selected answer indices
 */
export function castVote(
  election: Election,
  electionKey: ElectionPublicKey | undefined,
  selectedBallots: SelectedBallots,
): Promise<void> {
  const time: Timestamp = Timestamp.EpochNow();

  let votes: Vote[] | EncryptedVote[] = [];
  if (election.version === ElectionVersion.OPEN_BALLOT) {
    votes = CastVote.selectedBallotsToVotes(election, selectedBallots);
  } else if (election.version === ElectionVersion.SECRET_BALLOT) {
    if (!electionKey) {
      throw new Error(
        'castVote() was called on a secret ballot election without providing an encryption key',
      );
    }
    votes = CastVote.selectedBallotsToEncryptedVotes(election, electionKey, selectedBallots);
  } else {
    throw new Error(`castVote() was called using an unkwown election version ${election.version}`);
  }

  const message = new CastVote({
    lao: election.lao,
    election: election.id,
    created_at: time,
    votes,
  });

  // publish on the LAO channel specific to this election
  return publish(channelFromIds(election.lao, election.id), message);
}

/**
 * Sends a query to terminate an election.
 *
 * @param election - The election that should be terminated
 */
export function terminateElection(election: Election): Promise<void> {
  const time: Timestamp = Timestamp.EpochNow();
  const message = new EndElection({
    lao: election.lao,
    election: election.id,
    created_at: time,
    registered_votes: EndElection.computeRegisteredVotesHash(election),
  });

  // publish on the LAO channel specific to this election
  return publish(channelFromIds(election.lao, election.id), message);
}
