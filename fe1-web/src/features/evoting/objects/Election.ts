import { Hash, Timestamp } from 'core/objects';
import { LaoEventState } from 'features/events/objects/LaoEvent';

/**
 * Object to represent an election and all its components.
 */

export const EventTypeElection = 'ELECTION';

export interface ElectionState extends LaoEventState {
  lao: string;
  name: string;
  version: string;
  createdAt: number;
  start: number;
  end: number;
  questions: Question[];
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

export enum ElectionStatus {
  NOT_STARTED = 'not started',
  RUNNING = 'running',
  FINISHED = 'finished', // When the time is over
  TERMINATED = 'terminated', // When manually terminated by organizer
  RESULT = 'result', // When result is available
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
    // Sets the election status automatically
    this.electionStatus = Election.getElectionStatus(obj.start, obj.end);
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
    });
  }

  /**
   * Creates an ElectionState from the current Election object.
   */
  public toState(): ElectionState {
    const obj: any = JSON.parse(JSON.stringify(this));
    return {
      ...obj,
      eventType: EventTypeElection,
    };
  }

  /**
   * Gets the status of an election by knowing its start and end times.
   *
   * @param start - The start time of the election
   * @param end - The end time of the election
   * @private
   */
  private static getElectionStatus(start: Timestamp, end: Timestamp): ElectionStatus {
    const now = Timestamp.EpochNow();
    if (now.before(start)) {
      return ElectionStatus.NOT_STARTED;
    }
    if (now.before(end)) {
      return ElectionStatus.RUNNING;
    }
    return ElectionStatus.FINISHED;
  }
}
