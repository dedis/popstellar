import {
  Hash, Timestamp, Vote,
} from 'model/objects';
import { ProtocolError } from 'model/network/ProtocolError';
import { validateDataObject } from 'model/network/validation';
import { ActionType, MessageData, ObjectType } from '../MessageData';
import { checkTimestampStaleness } from '../Checker';
// “channel”: “/root/<lao_id>/<election_id>” // LAO-Election channel
// “message”: {
// “data”: base64({ /* Base 64 representation of the object
// 			“object”: “election”, // Constant
// 			“action”: “end”, // Constant
// 			“lao”: <lao_id>, // ID of the LAO
// 			“election”: <election_id>, // ID of the election
// 			“created_at”: <UNIX timestamp>, // Vote submitted time in UTC
// 			“registered_votes”: SHA256(<vote_id>, <vote_id>, ...),
// 			}),
// 		"sender": <base64>, /* Public key of organizer */
//     "signature": <base64>, /* Signature by organizer over "data" */
//     "message_id": <base64>, /* hash(data||signature) */
//     "witness_signatures": [], /*Signature by witnesses(sender||data)*/
//   },

export class EndElection implements MessageData {
  public readonly object: ObjectType = ObjectType.ELECTION;

  public readonly action: ActionType = ActionType.END;

  public readonly lao: Hash;

  public readonly election: Hash;

  public readonly created_at: Timestamp;

  public readonly votes: Vote[];

  constructor(msg: Partial<EndElection>) {
    if (!msg.election) {
      throw new ProtocolError('Undefined \'id\' parameter encountered during \'CastVote\'');
    }

    if (!msg.lao) {
      throw new ProtocolError('Undefined \'lao\' parameter encountered during \'CastVote\'');
    }
    this.lao = msg.lao;

    if (!msg.created_at) {
      throw new ProtocolError('Undefined \'created_at\' parameter encountered during \'CastVote\'');
    }
    checkTimestampStaleness(msg.created_at);
    this.created_at = msg.created_at;
    if (!msg.votes) {
      throw new ProtocolError('Undefined \'votes\' parameter encountered during \'CastVote\'');
    }
    EndElection.validateVotes(msg.votes);
    this.votes = msg.votes;

    if (!msg.election) {
      throw new ProtocolError('Invalid \'election\' parameter encountered during \'CastVote\'');
    }
    this.election = msg.election;
  }

  public static validateVotes(votes: Vote[]) {
    votes.forEach((vote) => {
      if (!vote.id) {
        throw new ProtocolError('Undefined \'vote id\' parameter encountered during \'CastVote\'');
      }
      if (!vote.question) {
        throw new ProtocolError('Undefined \'question id\' parameter encountered during \'CastVote\'');
      }
      if (!vote.vote && !vote.write_in) {
        throw new ProtocolError('Undefined \'vote or write in\' parameters encountered during \'CastVote\'');
      }
      if (vote.vote && vote.write_in) {
        throw new ProtocolError('Defined both \'vote\' and \'write_in\' parameters, only 1 is allowed, encountered during \'CastVote\'');
      }
    });
  }

  public static fromJson(obj: any): EndElection {
    const { errors } = validateDataObject(ObjectType.ELECTION, ActionType.END, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid cast vote\n\n${errors}`);
    }

    return new EndElection({
      ...obj,
      created_at: new Timestamp(obj.created_at),
      election: new Hash(obj.election),
      lao: new Hash(obj.lao),
    });
  }
}
