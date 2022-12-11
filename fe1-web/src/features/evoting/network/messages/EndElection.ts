import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { checkTimestampStaleness } from 'core/network/validation/Checker';
import { Hash, ProtocolError, Timestamp } from 'core/objects';
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
    const sortedVoteIds = [...election.registeredVotes]
      // Expand each registered vote to the contained vote ids
      // flatMap = map + flatten array
      .flatMap((registeredVote) => registeredVote.votes.map((vote) => vote.id))
      // Sort by vote ID
      // see https://github.com/dedis/popstellar/blob/master/docs/messageData.md#ending-an-election-electionend
      .sort((a, b) => (a.valueOf() < b.valueOf() ? -1 : 1));

    return Hash.fromArray(...sortedVoteIds);
  }
}
