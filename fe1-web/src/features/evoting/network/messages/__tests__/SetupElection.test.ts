import 'jest-extended';

import '__tests__/utils/matchers';
import { EventTags, Hash, Timestamp, ProtocolError } from 'core/objects';
import STRINGS from 'resources/strings';
import { mockLao, mockLaoId, mockLaoIdHash, mockLaoName } from '__tests__/utils/TestUtils';
import { OpenedLaoStore } from 'features/lao/store';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages/MessageData';

import { Question } from '../../../objects';
import { SetupElection } from '../SetupElection';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const VERSION = STRINGS.election_version_identifier;
const CLOSE_TIMESTAMP = new Timestamp(1609542000); // 2nd january 2021
const TIMESTAMP_BEFORE = new Timestamp(1609445600);

let electionId: Hash;
let mockQuestionObject1: Question;
let mockQuestionObject2: Question;
let sampleSetupElection: Partial<SetupElection>;

let setupElectionJson: string;
let mockQuestion1: string;
let mockQuestionId1: Hash;
let mockBallotOptions: string[];

const initializeData = () => {
  electionId = Hash.fromStringArray('Election', mockLaoId, TIMESTAMP.toString(), mockLaoName);
  mockQuestion1 = 'Mock Question 1';
  const mockQuestion2 = 'Mock Question 2';
  mockQuestionId1 = Hash.fromStringArray(EventTags.QUESTION, electionId.toString(), mockQuestion1);
  const mockQuestionId2 = Hash.fromStringArray(
    EventTags.QUESTION,
    electionId.toString(),
    mockQuestion2,
  );
  mockBallotOptions = ['Ballot Option 1', 'Ballot Option 2'];

  mockQuestionObject1 = {
    id: mockQuestionId1.toString(),
    question: mockQuestion1,
    voting_method: STRINGS.election_method_Plurality,
    ballot_options: mockBallotOptions,
    write_in: false,
  };

  mockQuestionObject2 = {
    id: mockQuestionId2.toString(),
    question: mockQuestion2,
    voting_method: STRINGS.election_method_Approval,
    ballot_options: mockBallotOptions,
    write_in: true,
  };

  const mockQuestions = [mockQuestionObject1];

  sampleSetupElection = {
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

  setupElectionJson = `{
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
};

beforeAll(() => {
  initializeData();
  OpenedLaoStore.store(mockLao);
});

describe('SetupElection', () => {
  it('should be created correctly from Json', () => {
    expect(new SetupElection(sampleSetupElection)).toBeJsonEqual(sampleSetupElection);
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
    const createFromJson = () => SetupElection.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if id is undefined', () => {
      const createWrongObj = () =>
        new SetupElection({
          object: ObjectType.ELECTION,
          action: ActionType.SETUP,
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
          object: ObjectType.ELECTION,
          action: ActionType.SETUP,
          id: electionId,
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
          object: ObjectType.ELECTION,
          action: ActionType.SETUP,
          id: electionId,
          lao: mockLaoIdHash,
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
          object: ObjectType.ELECTION,
          action: ActionType.SETUP,
          id: electionId,
          lao: mockLaoIdHash,
          name: mockLaoName,
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
          object: ObjectType.ELECTION,
          action: ActionType.SETUP,
          id: electionId,
          lao: mockLaoIdHash,
          name: mockLaoName,
          version: VERSION,
          start_time: TIMESTAMP,
          end_time: CLOSE_TIMESTAMP,
          questions: [mockQuestionObject1, mockQuestionObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if start_time is undefined', () => {
      const createWrongObj = () =>
        new SetupElection({
          object: ObjectType.ELECTION,
          action: ActionType.SETUP,
          id: electionId,
          lao: mockLaoIdHash,
          name: mockLaoName,
          version: VERSION,
          created_at: TIMESTAMP,
          end_time: CLOSE_TIMESTAMP,
          questions: [mockQuestionObject1, mockQuestionObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if end_time is undefined', () => {
      const createWrongObj = () =>
        new SetupElection({
          object: ObjectType.ELECTION,
          action: ActionType.SETUP,
          id: electionId,
          lao: mockLaoIdHash,
          name: mockLaoName,
          version: VERSION,
          created_at: TIMESTAMP,
          start_time: TIMESTAMP,
          questions: [mockQuestionObject1, mockQuestionObject2],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if questions is undefined', () => {
      const createWrongObj = () =>
        new SetupElection({
          object: ObjectType.ELECTION,
          action: ActionType.SETUP,
          id: electionId,
          lao: mockLaoIdHash,
          name: mockLaoName,
          version: VERSION,
          created_at: TIMESTAMP,
          start_time: TIMESTAMP,
          end_time: CLOSE_TIMESTAMP,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if start_time is before created_at', () => {
      const createWrongObj = () =>
        new SetupElection({
          object: ObjectType.ELECTION,
          action: ActionType.SETUP,
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
          object: ObjectType.ELECTION,
          action: ActionType.SETUP,
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

  it('should throw an error if id is undefined', () => {
    const createWrongObj = () =>
      new SetupElection({
        object: ObjectType.ELECTION,
        action: ActionType.SETUP,
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
