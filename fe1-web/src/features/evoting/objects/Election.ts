import { Hash, HashState, Timestamp, TimestampState } from 'core/objects';

import { Question, QuestionState } from './Question';
import { QuestionResult, QuestionResultState } from './QuestionResult';
import { RegisteredVote, RegisteredVoteState } from './RegisteredVote';

/**
 * Object to represent an election and all its components.
 */

export enum ElectionStatus {
  NOT_STARTED = 'not started',
  OPENED = 'opened',
  TERMINATED = 'terminated', // When manually terminated by organizer
  RESULT = 'result', // When result is available
}

export enum ElectionVersion {
  OPEN_BALLOT = 'OPEN_BALLOT',
  SECRET_BALLOT = 'SECRET_BALLOT',
}

export interface ElectionState {
  id: HashState;
  lao: HashState;
  name: string;
  version: ElectionVersion;
  createdAt: TimestampState;
  start: TimestampState;
  end: TimestampState;
  questions: QuestionState[];
  electionStatus: ElectionStatus;
  registeredVotes: RegisteredVoteState[];
  questionResult?: QuestionResultState[];
}

// This type ensures that for each question there is a unique set of option indices
export type SelectedBallots = { [questionIndex: number]: number };

export class Election {
  public static EVENT_TYPE = 'ELECTION';

  public readonly lao: Hash;

  public readonly id: Hash;

  public readonly name: string;

  public readonly version: ElectionVersion;

  public readonly createdAt: Timestamp;

  public readonly start: Timestamp;

  public readonly end: Timestamp;

  public readonly questions: Question[];

  public electionStatus: ElectionStatus;

  public registeredVotes: RegisteredVote[];

  public questionResult?: QuestionResult[];

  constructor(obj: Partial<Election>) {
    if (obj === undefined || obj === null) {
      throw new Error(
        'Error encountered while creating a Election object: undefined/null parameters',
      );
    }
    if (obj.lao === undefined) {
      throw new Error("Undefined 'lao' when creating 'Election'");
    }
    if (obj.id === undefined) {
      throw new Error("Undefined 'id' when creating 'Election'");
    }
    if (obj.name === undefined) {
      throw new Error("Undefined 'name' when creating 'Election'");
    }
    if (obj.version === undefined) {
      throw new Error("Undefined 'version' when creating 'Election'");
    }
    if (obj.createdAt === undefined) {
      throw new Error("Undefined 'creation' when creating 'Election'");
    }
    if (obj.start === undefined) {
      throw new Error("Undefined 'start' when creating 'Election'");
    }
    if (obj.end === undefined) {
      throw new Error("Undefined 'end' when creating 'Election'");
    }
    if (obj.questions === undefined) {
      throw new Error("Undefined 'questions' when creating 'Election'");
    }
    if (obj.electionStatus === undefined) {
      throw new Error("Undefined 'electionStatus' when creating 'Election'");
    }
    if (obj.registeredVotes === undefined) {
      this.registeredVotes = [];
    } else {
      this.registeredVotes = obj.registeredVotes;
    }

    this.lao = obj.lao;
    this.id = obj.id;
    this.name = obj.name;
    this.version = obj.version;
    this.createdAt = obj.createdAt;
    this.start = obj.start;
    this.end = obj.end;
    this.questions = obj.questions;
    this.questionResult = obj.questionResult;
    this.electionStatus = obj.electionStatus;
  }

  /**
   * Creates an ElectionState from the current Election object.
   */
  public toState(): ElectionState {
    return {
      lao: this.lao.toState(),
      id: this.id.toState(),
      name: this.name,
      version: this.version,
      createdAt: this.createdAt.toState(),
      start: this.start.toState(),
      end: this.end.toState(),
      questions: this.questions.map((q) => q.toState()),
      registeredVotes: this.registeredVotes.map((vote) => vote.toState()),
      electionStatus: this.electionStatus,
      questionResult: this.questionResult?.map((result) => result.toState()),
    };
  }

  /**
   * Creates an Election from an ElectionState.
   *
   * @param electionState
   */
  public static fromState(electionState: ElectionState): Election {
    return new Election({
      lao: Hash.fromState(electionState.lao),
      id: Hash.fromState(electionState.id),
      name: electionState.name,
      version: electionState.version,
      createdAt: Timestamp.fromState(electionState.createdAt),
      start: Timestamp.fromState(electionState.start),
      end: Timestamp.fromState(electionState.end),
      questions: electionState.questions.map(Question.fromState),
      registeredVotes: electionState.registeredVotes.map(RegisteredVote.fromState),
      electionStatus: electionState.electionStatus,
      questionResult: electionState.questionResult?.map(QuestionResult.fromState),
    });
  }
}
