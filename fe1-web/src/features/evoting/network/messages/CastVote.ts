import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { checkTimestampStaleness } from 'core/network/validation/Checker';
import { EventTags, Hash, ProtocolError, Timestamp } from 'core/objects';
import { MessageDataProperties } from 'core/types';
import { ElectionPublicKey } from 'features/evoting/objects/ElectionPublicKey';

import { Election, ElectionVersion, EncryptedVote, SelectedBallots, Vote } from '../../objects';

// we are using a 2 byte unsigned number to represent the option index
const MAX_OPTION_INDEX = 2 ** (2 /* #bytes */ * 8); /* bits in a byte */

/** Data sent to cast a vote */
export class CastVote implements MessageData {
  public readonly object: ObjectType = ObjectType.ELECTION;

  public readonly action: ActionType = ActionType.CAST_VOTE;

  public readonly lao: Hash;

  public readonly election: Hash;

  public readonly created_at: Timestamp;

  public readonly votes: Vote[] | EncryptedVote[];

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
  public static validateVotes(votes: Vote[] | EncryptedVote[]) {
    votes.forEach((vote) => {
      if (!vote.id) {
        throw new ProtocolError("Undefined 'vote id' parameter encountered during 'CastVote'");
      }
      if (!vote.question) {
        throw new ProtocolError("Undefined 'question id' parameter encountered during 'CastVote'");
      }
      if (vote.vote === undefined) {
        throw new ProtocolError("Undefined 'vote' parameters encountered during 'CastVote'");
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
   * Converts an object of type SelectedBallots to an array of votes ready to be passed to a CastVote message
   * @param election The election for which these votes are cast
   * @param selectedBallots The selected ballot options
   * @returns An array of votes that can be passed to a CastVote message
   */
  public static selectedBallotsToVotes(
    election: Election,
    selectedBallots: SelectedBallots,
  ): Vote[] {
    if (election.version !== ElectionVersion.OPEN_BALLOT) {
      throw new Error('selectedBallotsToVotes() should only be called on open ballot elections');
    }
    // Convert object to array
    const ballotArray = Object.entries(selectedBallots).map(([idx, selectedOptionIndex]) => ({
      index: parseInt(idx, 10),
      selectedOptionIndex,
    }));

    // sort the answers by index
    ballotArray.sort((a, b) => a.index - b.index);

    return (
      ballotArray
        // and add an id to all votes as well as the matching question id
        .map<Vote>(
          ({ index, selectedOptionIndex }) =>
            new Vote({
              // generate the vote id
              id: CastVote.computeVoteId(election, index, selectedOptionIndex),
              // find matching question id from the election
              question: election.questions[index].id,
              // convert the set to an array and sort votes in ascending order
              vote: selectedOptionIndex,
            }),
        )
    );
  }

  /**
   * Converts an object of type SelectedBallots to an array of encrypted votes ready to be passed to a CastVote message
   * @param election The election for which these votes are cast
   * @param electionKey The public key of the election or undefined if there is none
   * @param selectedBallots The selected ballot options
   * @returns An array of encrypted votes that can be passed to a CastVote message
   */
  public static selectedBallotsToEncryptedVotes(
    election: Election,
    electionKey: ElectionPublicKey,
    selectedBallots: SelectedBallots,
  ): EncryptedVote[] {
    if (election.version !== ElectionVersion.SECRET_BALLOT) {
      throw new Error(
        'selectedBallotsToEncryptedVotes() should only be called on secret ballot elections',
      );
    }

    // Convert object to array
    const ballotArray = Object.entries(selectedBallots).map(([idx, selectedOptionIndex]) => ({
      index: parseInt(idx, 10),
      selectedOptionIndex,
    }));

    // sort the answers by index
    ballotArray.sort((a, b) => a.index - b.index);

    return (
      ballotArray
        // and add an id to all votes as well as the matching question id
        .map<EncryptedVote>(({ index, selectedOptionIndex }) => {
          if (selectedOptionIndex >= MAX_OPTION_INDEX) {
            throw new Error(
              `The selected option index ${selectedOptionIndex} is greater than ${MAX_OPTION_INDEX}. This is not supported`,
            );
          }

          // allocUnsafe is fine, we will overwrite all bytes
          const buffer = Buffer.allocUnsafe(2);
          // write 2 bytes using big endian
          buffer.writeIntBE(selectedOptionIndex, 0, 2);

          const encryptedOptionIndex = electionKey.encrypt(buffer).valueOf();

          return new EncryptedVote({
            // generate the vote id based on the **encrypted** option indices
            id: CastVote.computeSecretVoteId(election, index, encryptedOptionIndex),
            // find matching question id from the election
            question: election.questions[index].id,
            // use the encrypted votes
            vote: encryptedOptionIndex,
          });
        })
    );
  }

  /**
   * Generates a vote id as described in
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataCastVote.json
   * @param election The election for which this vote id is generated
   * @param questionIndex The index of the question in the given election, this vote is cast for
   * @param selectionOptionIndex The selected option index
   */
  public static computeVoteId(
    election: Election,
    questionIndex: number,
    selectionOptionIndex: number,
  ): Hash {
    return Hash.fromStringArray(
      EventTags.VOTE,
      election.id.toString(),
      election.questions[questionIndex].id.valueOf(),
      selectionOptionIndex.toString(),
    );
  }

  /**
   * Generates a vote id for a secret ballot election as described in
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataCastVote.json
   * @param election The election for which this vote id is generated
   * @param questionIndex The index of the question in the given election, this vote is cast for
   * @param encryptedOptionIndex The encrypted selected option indices
   */
  public static computeSecretVoteId(
    election: Election,
    questionIndex: number,
    encryptedOptionIndex: string,
  ): Hash {
    return Hash.fromStringArray(
      EventTags.VOTE,
      election.id.toString(),
      election.questions[questionIndex].id.valueOf(),
      encryptedOptionIndex,
    );
  }
}
