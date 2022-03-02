import 'jest-extended';
import '__tests__/utils/matchers';
import {
  mockLao,
  mockLaoId,
  mockLaoIdHash,
  mockLaoName,
  configureTestFeatures,
} from '__tests__/utils';

import { EventTags, Hash, Timestamp, ProtocolError } from 'core/objects';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { OpenedLaoStore } from 'features/lao/store';
import STRINGS from 'resources/strings';

import { MessageDataProperties } from 'core/types';
import { Question } from '../../../objects';
import { SetupElection } from '../SetupElection';

// region test data initialization

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const VERSION = STRINGS.election_version_identifier;
const CLOSE_TIMESTAMP = new Timestamp(1609542000); // 2nd january 2021
const TIMESTAMP_BEFORE = new Timestamp(1609445600);

const electionId: Hash = Hash.fromStringArray(
  'Election',
  mockLaoId,
  TIMESTAMP.toString(),
  mockLaoName,
);

const mockQuestion1: string = 'Mock Question 1';
const mockQuestion2 = 'Mock Question 2';

const mockQuestionId1: Hash = Hash.fromStringArray(
  EventTags.QUESTION,
  electionId.toString(),
  mockQuestion1,
);
const mockQuestionId2 = Hash.fromStringArray(
  EventTags.QUESTION,
  electionId.toString(),
  mockQuestion2,
);

const mockBallotOptions = ['Ballot Option 1', 'Ballot Option 2'];

const mockQuestionObject1: Question = {
  id: mockQuestionId1.toString(),
  question: mockQuestion1,
  voting_method: STRINGS.election_method_Plurality,
  ballot_options: mockBallotOptions,
  write_in: false,
};

const mockQuestionObject2: Question = {
  id: mockQuestionId2.toString(),
  question: mockQuestion2,
  voting_method: STRINGS.election_method_Approval,
  ballot_options: mockBallotOptions,
  write_in: true,
};

const mockQuestions = [mockQuestionObject1];

// In these tests, we should assume that the input to the messages is
// just a Partial<> and not a MessageDataProperties<>
// as this will catch more issues at runtime. (Defensive programming)
const sampleSetupElection: Partial<SetupElection> = {
  object: ObjectType.ELECTION,
  action: ActionType.SETUP,
  id: electionId,
  lao: mockLaoIdHash,
  name: mockLaoName,
  version: VERSION,
  created_at: TIMESTAMP,
  start_time: TIMESTAMP,
  end_time: CLOSE_TIMESTAMP,
  questions: mockQuestions,
};

const setupElectionJson: string = `{
  "object": "${ObjectType.ELECTION}",
  "action": "${ActionType.SETUP}",
  "id": "${electionId}",
  "lao": "${mockLaoIdHash}",
  "name": "${mockLaoName}",
  "version": "${VERSION}",
  "created_at": ${TIMESTAMP},
  "start_time": ${TIMESTAMP},
  "end_time": ${CLOSE_TIMESTAMP},
  "questions": ${JSON.stringify(mockQuestions)}
}`;

// endregion

beforeAll(() => {
  configureTestFeatures();
  OpenedLaoStore.store(mockLao);
});

describe('SetupElection', () => {
  it('should be created correctly from Json', () => {
    expect(
      new SetupElection(sampleSetupElection as MessageDataProperties<SetupElection>),
    ).toBeJsonEqual(sampleSetupElection);
    const temp = {
      object: ObjectType.ELECTION,
      action: ActionType.SETUP,
      id: electionId,
      lao: mockLaoIdHash,
      name: mockLaoName,
      version: VERSION,
      created_at: TIMESTAMP,
      start_time: TIMESTAMP,
      end_time: CLOSE_TIMESTAMP,
      questions: [mockQuestionObject1, mockQuestionObject2],
    };
    expect(new SetupElection(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(setupElectionJson);
    expect(SetupElection.fromJson(obj)).toBeJsonEqual(sampleSetupElection);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.ELECTION,
      action: ActionType.NOTIFY_ADD,
      id: electionId.toString(),
      lao: mockLaoIdHash.toString(),
      name: mockLaoName,
      version: VERSION,
      created_at: parseInt(TIMESTAMP.toString(), 10),
      start_time: parseInt(TIMESTAMP.toString(), 10),
      end_time: parseInt(CLOSE_TIMESTAMP.toString(), 10),
      questions: [mockQuestionObject1, mockQuestionObject2],
    };
    const createFromJson = () => SetupElection.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  it('fromJson should throw an error if the Json has incorrect object', () => {
    const obj = {
      object: ObjectType.CHIRP,
      action: ActionType.SETUP,
      id: electionId.toString(),
      lao: mockLaoIdHash.toString(),
      name: mockLaoName,
      version: VERSION,
      created_at: parseInt(TIMESTAMP.toString(), 10),
      start_time: parseInt(TIMESTAMP.toString(), 10),
      end_time: parseInt(CLOSE_TIMESTAMP.toString(), 10),
      questions: [mockQuestionObject1, mockQuestionObject2],
    };
    const createFromJson = () => SetupElection.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if id is undefined', () => {
      const createWrongObj = () =>
        new SetupElection({
          id: undefined as unknown as Hash,
          lao: mockLaoIdHash,
          name: mockLaoName,
          version: VERSION,
          created_at: TIMESTAMP,
          start_time: TIMESTAMP,
          end_time: CLOSE_TIMESTAMP,
          questions: [mockQuestionObject1, mockQuestionObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if lao is undefined', () => {
      const createWrongObj = () =>
        new SetupElection({
          id: electionId,
          lao: undefined as unknown as Hash,
          name: mockLaoName,
          version: VERSION,
          created_at: TIMESTAMP,
          start_time: TIMESTAMP,
          end_time: CLOSE_TIMESTAMP,
          questions: [mockQuestionObject1, mockQuestionObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if name is undefined', () => {
      const createWrongObj = () =>
        new SetupElection({
          id: electionId,
          lao: mockLaoIdHash,
          name: undefined as unknown as string,
          version: VERSION,
          created_at: TIMESTAMP,
          start_time: TIMESTAMP,
          end_time: CLOSE_TIMESTAMP,
          questions: [mockQuestionObject1, mockQuestionObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if version is undefined', () => {
      const createWrongObj = () =>
        new SetupElection({
          id: electionId,
          lao: mockLaoIdHash,
          name: mockLaoName,
          version: undefined as unknown as string,
          created_at: TIMESTAMP,
          start_time: TIMESTAMP,
          end_time: CLOSE_TIMESTAMP,
          questions: [mockQuestionObject1, mockQuestionObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if created_at is undefined', () => {
      const createWrongObj = () =>
        new SetupElection({
          id: electionId,
          lao: mockLaoIdHash,
          name: mockLaoName,
          version: VERSION,
          created_at: undefined as unknown as Timestamp,
          start_time: TIMESTAMP,
          end_time: CLOSE_TIMESTAMP,
          questions: [mockQuestionObject1, mockQuestionObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if start_time is undefined', () => {
      const createWrongObj = () =>
        new SetupElection({
          id: electionId,
          lao: mockLaoIdHash,
          name: mockLaoName,
          version: VERSION,
          created_at: TIMESTAMP,
          start_time: undefined as unknown as Timestamp,
          end_time: CLOSE_TIMESTAMP,
          questions: [mockQuestionObject1, mockQuestionObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if end_time is undefined', () => {
      const createWrongObj = () =>
        new SetupElection({
          id: electionId,
          lao: mockLaoIdHash,
          name: mockLaoName,
          version: VERSION,
          created_at: TIMESTAMP,
          start_time: TIMESTAMP,
          end_time: undefined as unknown as Timestamp,
          questions: [mockQuestionObject1, mockQuestionObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if questions is undefined', () => {
      const createWrongObj = () =>
        new SetupElection({
          id: electionId,
          lao: mockLaoIdHash,
          name: mockLaoName,
          version: VERSION,
          created_at: TIMESTAMP,
          start_time: TIMESTAMP,
          end_time: CLOSE_TIMESTAMP,
          questions: undefined as unknown as Question[],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if start_time is before created_at', () => {
      const createWrongObj = () =>
        new SetupElection({
          id: electionId,
          lao: mockLaoIdHash,
          name: mockLaoName,
          version: VERSION,
          created_at: TIMESTAMP,
          start_time: TIMESTAMP_BEFORE,
          end_time: CLOSE_TIMESTAMP,
          questions: [mockQuestionObject1, mockQuestionObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if end_time is before start_time', () => {
      const createWrongObj = () =>
        new SetupElection({
          id: electionId,
          lao: mockLaoIdHash,
          name: mockLaoName,
          version: VERSION,
          created_at: TIMESTAMP_BEFORE,
          start_time: TIMESTAMP,
          end_time: TIMESTAMP_BEFORE,
          questions: [mockQuestionObject1, mockQuestionObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });
  });

  it('should ignore passed object and action parameters', () => {
    const msg = new SetupElection({
      object: ObjectType.CHIRP,
      action: ActionType.NOTIFY_ADD,
      id: electionId,
      lao: mockLaoIdHash,
      name: mockLaoName,
      version: VERSION,
      created_at: TIMESTAMP,
      start_time: TIMESTAMP,
      end_time: CLOSE_TIMESTAMP,
      questions: [mockQuestionObject1, mockQuestionObject2],
    } as MessageDataProperties<SetupElection>);

    expect(msg.object).toEqual(ObjectType.ELECTION);
    expect(msg.action).toEqual(ActionType.SETUP);
  });

  describe('validateQuestions', () => {
    it('should throw an error if the question id is wrong', () => {
      const wrongQuestion: Question = {
        id: 'id',
        question: mockQuestion1,
        voting_method: STRINGS.election_method_Plurality,
        ballot_options: mockBallotOptions,
        write_in: false,
      };
      const wrongValidate = () => {
        SetupElection.validateQuestions([wrongQuestion], electionId.valueOf());
      };
      expect(wrongValidate).toThrow(ProtocolError);
    });
  });
});
