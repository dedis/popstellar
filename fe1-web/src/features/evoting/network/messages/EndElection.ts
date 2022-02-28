import { Hash, Timestamp, ProtocolError } from 'core/objects';
import { validateDataObject } from 'core/network/validation';
import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { checkTimestampStaleness } from 'core/network/validation/Checker';
import { MessageDataProperties } from 'core/types';
import { Election } from 'features/evoting/objects';

/** Data sent to end an Election event */
export class EndElection implements MessageData {
  public readonly object: ObjectType = ObjectType.ELECTION;

  public readonly action: ActionType = ActionType.END;

  public readonly lao: Hash;

  public readonly election: Hash;

  public readonly created_at: Timestamp;

  public readonly registered_votes: Hash;

  constructor(msg: MessageDataProperties<EndElection>) {
    if (!msg.lao) {
      throw new ProtocolError("Undefined 'lao' parameter encountered during 'EndElection'");
    }
    this.lao = msg.lao;

    if (!msg.created_at) {
      throw new ProtocolError("Undefined 'created_at' parameter encountered during 'EndElection'");
    }
    checkTimestampStaleness(msg.created_at);
    this.created_at = msg.created_at;
    if (!msg.registered_votes) {
      throw new ProtocolError(
        "Undefined 'registered_votes' parameter encountered during 'EndElection'",
      );
    }

    this.registered_votes = msg.registered_votes;

    if (!msg.election) {
      throw new ProtocolError("Invalid 'election' parameter encountered during 'EndElection'");
    }
    this.election = msg.election;
  }

  /**
   * Creates an EndElection object from a given object.
   *
   * @param obj
   */
  public static fromJson(obj: any): EndElection {
    const { errors } = validateDataObject(ObjectType.ELECTION, ActionType.END, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid election end\n\n${errors}`);
    }

    return new EndElection({
      ...obj,
      created_at: new Timestamp(obj.created_at),
      election: new Hash(obj.election),
      lao: new Hash(obj.lao),
    });
  }

  public static computeRegisteredVotesHash(election: Election) {
    const sortedVoteIds = election.registeredVotes
      // First sort by timestamp, than by message ID as tiebreaker
      .sort((a, b) => {
        const tiebreaker = a.messageId.valueOf() < b.messageId.valueOf() ? -1 : 1;
        return a !== b ? a.createdAt - b.createdAt : tiebreaker;
      })
      // Now expand each registered vote to the contained vote ids
      // flatMap = map + flatten array
      .flatMap((registeredVote) => registeredVote.votes.map((vote) => vote.id));

    return Hash.fromStringArray(...sortedVoteIds);
  }
}
