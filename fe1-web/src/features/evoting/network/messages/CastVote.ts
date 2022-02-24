import { Hash, Timestamp, ProtocolError } from 'core/objects';
import { validateDataObject } from 'core/network/validation';
import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { checkTimestampStaleness } from 'core/network/validation/Checker';

import { MessageDataProperties } from 'core/types';
import { Vote } from '../../objects';

/** Data sent to cast a vote */
export class CastVote implements MessageData {
  public readonly object: ObjectType = ObjectType.ELECTION;

  public readonly action: ActionType = ActionType.CAST_VOTE;

  public readonly lao: Hash;

  public readonly election: Hash;

  public readonly created_at: Timestamp;

  public readonly votes: Vote[];

  constructor(msg: MessageDataProperties<CastVote>) {
    this.lao = msg.lao;
    this.election = msg.election;

    checkTimestampStaleness(msg.created_at);
    this.created_at = msg.created_at;

    CastVote.validateVotes(msg.votes);
    this.votes = msg.votes;
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
}
