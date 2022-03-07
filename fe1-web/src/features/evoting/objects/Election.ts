import { Hash, Timestamp } from 'core/objects';
import { EvotingFeature } from './Feature';

/**
 * Object to represent an election and all its components.
 */

export const ELECTION_EVENT_TYPE = 'ELECTION';

export enum ElectionStatus {
  NOT_STARTED = 'not started',
  OPENED = 'opened',
  TERMINATED = 'terminated', // When manually terminated by organizer
  RESULT = 'result', // When result is available
}

export interface ElectionState extends EvotingFeature.EventState {
  lao: string;
  name: string;
  version: string;
  createdAt: number;
  start: number;
  end: number;
  questions: Question[];
  electionStatus: ElectionStatus;
  registeredVotes: RegisteredVote[];
  questionResult?: QuestionResult[];
}

export interface Question {
  id: string;
  question: string;
  voting_method: string;
  ballot_options: string[];
  write_in: boolean;
}

export interface Vote {
  id: string;
  question: string;
  vote?: number[];
  writeIn?: string;
}

// This type ensures that for each question there is a unique set of option indices
export type SelectedBallots = { [questionIndex: number]: Set<number> };

export interface RegisteredVote {
  createdAt: number;
  sender: string;
  votes: Vote[];
  messageId: string;
}

export interface MajorityResult {
  ballotOption: string;
  count: number;
}

export interface QuestionResult {
  id: string;
  result: MajorityResult[];
}

export class Election {
  public readonly lao: Hash;

  public readonly id: Hash;

  public readonly name: string;

  public readonly version: string;

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
   * Creates an Election from an ElectionState.
   *
   * @param electionState
   */
  public static fromState(electionState: ElectionState): Election {
    return new Election({
      lao: new Hash(electionState.lao),
      id: new Hash(electionState.id),
      name: electionState.name,
      version: electionState.version,
      createdAt: new Timestamp(electionState.createdAt),
      start: new Timestamp(electionState.start),
      end: new Timestamp(electionState.end),
      questions: electionState.questions,
      registeredVotes: electionState.registeredVotes,
      electionStatus: electionState.electionStatus,
      questionResult: electionState.questionResult,
    });
  }

  /**
   * Creates an ElectionState from the current Election object.
   */
  public toState(): ElectionState {
    const obj: any = JSON.parse(JSON.stringify(this));
    return {
      ...obj,
      eventType: ELECTION_EVENT_TYPE,
    };
  }
}
