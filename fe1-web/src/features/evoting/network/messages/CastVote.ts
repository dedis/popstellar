import { Hash, Timestamp, ProtocolError, EventTags } from 'core/objects';
import { validateDataObject } from 'core/network/validation';
import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { checkTimestampStaleness } from 'core/network/validation/Checker';

import { MessageDataProperties } from 'core/types';
import { Election, Vote } from '../../objects';

/** Data sent to cast a vote */
export class CastVote implements MessageData {
  public readonly object: ObjectType = ObjectType.ELECTION;

  public readonly action: ActionType = ActionType.CAST_VOTE;

  public readonly lao: Hash;

  public readonly election: Hash;

  public readonly created_at: Timestamp;

  public readonly votes: Vote[];

  constructor(msg: MessageDataProperties<CastVote>) {
    if (!msg.election) {
      throw new ProtocolError("Undefined 'id' parameter encountered during 'CastVote'");
    }

    if (!msg.lao) {
      throw new ProtocolError("Undefined 'lao' parameter encountered during 'CastVote'");
    }
    this.lao = msg.lao;

    if (!msg.created_at) {
      throw new ProtocolError("Undefined 'created_at' parameter encountered during 'CastVote'");
    }
    checkTimestampStaleness(msg.created_at);
    this.created_at = msg.created_at;
    if (!msg.votes) {
      throw new ProtocolError("Undefined 'votes' parameter encountered during 'CastVote'");
    }
    CastVote.validateVotes(msg.votes);
    this.votes = msg.votes;

    if (!msg.election) {
      throw new ProtocolError("Invalid 'election' parameter encountered during 'CastVote'");
    }
    this.election = msg.election;
  }

  /**
   * Checks the validity of an array of votes.
   *
   * @param votes - The array of votes to be checked
   */
  public static validateVotes(votes: Vote[]) {
    votes.forEach((vote) => {
      if (!vote.id) {
        throw new ProtocolError("Undefined 'vote id' parameter encountered during 'CastVote'");
      }
      if (!vote.question) {
        throw new ProtocolError("Undefined 'question id' parameter encountered during 'CastVote'");
      }
      if (!vote.vote && !vote.writeIn) {
        throw new ProtocolError(
          "Undefined 'vote or write in' parameters encountered during 'CastVote'",
        );
      }
      if (vote.vote && vote.writeIn) {
        throw new ProtocolError(
          "Defined both 'vote' and 'write_in' parameters, only 1 is allowed, encountered during 'CastVote'",
        );
      }
    });
  }

  /**
   * Creates a CastVote object from a given object
   * @param obj
   */
  public static fromJson(obj: any): CastVote {
    const { errors } = validateDataObject(ObjectType.ELECTION, ActionType.CAST_VOTE, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid cast vote\n\n${errors}`);
    }

    return new CastVote({
      ...obj,
      created_at: new Timestamp(obj.created_at),
      election: new Hash(obj.election),
      lao: new Hash(obj.lao),
    });
  }

  /**
   * Generates a vote id as described in
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataCastVote.json
   * HashLen('Vote', election_id, question_id, (vote_index(es)|write_in)), concatenate vote indexes - must use delimiter
   * @param election The election for which this vote id is generated
   * @param questionIndex The index of the question in the given election, this vote is cast for
   * @param selectionOptions The selected answers for the given question in the given eeleciton
   */
  public static generateVoteId(
    election: Election,
    questionIndex: number,
    selectionOptions: Set<number>,
  ): Hash {
    return Hash.fromStringArray(
      EventTags.VOTE,
      election.id.toString(),
      election.questions[questionIndex].id,
      // Important: A standardized order is required, otherwise the hash cannot be verified
      // Even more important: A standardized delimiter has to be used to disambiguate [1,0] from [10]
      // See https://github.com/dedis/popstellar/issues/843 for details
      // TODO: Update after discussion in #843 is finished
      [...selectionOptions]
        // sort in ascending order
        .sort((a, b) => a - b)
        // convert to strings
        .map((idx) => idx.toString())
        // concatenate and add delimiter in between strings
        .join(','),
    );
  }
}
