import 'jest-extended';
import '__tests__/utils/matchers';

import {
  configureTestFeatures,
  mockKeyPair,
  mockLaoId,
  mockLaoIdHash,
  mockLaoName,
  mockPublicKey,
} from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, PublicKey, Timestamp } from 'core/objects';
import { MessageDataProperties } from 'core/types';
import {
  mockBallotOptions,
  mockElectionId,
  mockQuestion1,
  mockQuestionObject1,
  mockQuestionObject2,
  mockQuestions,
} from 'features/evoting/__tests__/utils';
import STRINGS from 'resources/strings';

import { Question } from '../../../objects';
import { SetupElection } from '../SetupElection';

// region test data initialization

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const OPEN_BALLOT_VERSION = STRINGS.election_version_open_ballot;
const SECRET_BALLOT_VERSION = STRINGS.election_version_secret_ballot;
const CLOSE_TIMESTAMP = new Timestamp(1609542000); // 2nd january 2021
const TIMESTAMP_BEFORE = new Timestamp(1609445600);

// In these tests, we should assume that the input to the messages is
// just a Partial<> and not a MessageDataProperties<>
// as this will catch more issues at runtime. (Defensive programming)
const sampleSetupElectionOpenBallot: Partial<SetupElection> = {
  object: ObjectType.ELECTION,
  action: ActionType.SETUP,
  id: mockElectionId,
  lao: mockLaoIdHash,
  name: mockLaoName,
  version: OPEN_BALLOT_VERSION,
  created_at: TIMESTAMP,
  start_time: TIMESTAMP,
  end_time: CLOSE_TIMESTAMP,
  questions: mockQuestions,
};

const setupElectionOpenBallotJson: string = `{
  "object": "${ObjectType.ELECTION}",
  "action": "${ActionType.SETUP}",
  "id": "${mockElectionId.valueOf()}",
  "lao": "${mockLaoId}",
  "name": "${mockLaoName}",
  "version": "${OPEN_BALLOT_VERSION}",
  "created_at": ${TIMESTAMP.valueOf()},
  "start_time": ${TIMESTAMP.valueOf()},
  "end_time": ${CLOSE_TIMESTAMP.valueOf()},
  "questions": ${JSON.stringify(mockQuestions)}
}`;

const sampleSetupElectionSecretBallot: Partial<SetupElection> = {
  object: ObjectType.ELECTION,
  action: ActionType.SETUP,
  id: mockElectionId,
  lao: mockLaoIdHash,
  name: mockLaoName,
  key: mockKeyPair.publicKey,
  version: SECRET_BALLOT_VERSION,
  created_at: TIMESTAMP,
  start_time: TIMESTAMP,
  end_time: CLOSE_TIMESTAMP,
  questions: mockQuestions,
};

const setupElectionSecretBallotJson: string = `{
  "object": "${ObjectType.ELECTION}",
  "action": "${ActionType.SETUP}",
  "id": "${mockElectionId.valueOf()}",
  "lao": "${mockLaoId}",
  "name": "${mockLaoName}",
  "key": "${mockPublicKey.valueOf()}",
  "version": "${SECRET_BALLOT_VERSION}",
  "created_at": ${TIMESTAMP.valueOf()},
  "start_time": ${TIMESTAMP.valueOf()},
  "end_time": ${CLOSE_TIMESTAMP.valueOf()},
  "questions": ${JSON.stringify(mockQuestions)}
}`;

// endregion

beforeAll(() => {
  configureTestFeatures();
});

describe('SetupElection', () => {
  describe(`${OPEN_BALLOT_VERSION}`, () => {
    it('should be created correctly from Json', () => {
      expect(
        new SetupElection(sampleSetupElectionOpenBallot as MessageDataProperties<SetupElection>),
      ).toEqual(sampleSetupElectionOpenBallot);
      const temp = {
        object: ObjectType.ELECTION,
        action: ActionType.SETUP,
        version: OPEN_BALLOT_VERSION,
        id: mockElectionId,
        lao: mockLaoIdHash,
        name: mockLaoName,
        created_at: TIMESTAMP,
        start_time: TIMESTAMP,
        end_time: CLOSE_TIMESTAMP,
        questions: [mockQuestionObject1, mockQuestionObject2],
      };
      expect(new SetupElection(temp)).toBeJsonEqual(temp);
    });

    it('should be parsed correctly from Json', () => {
      const obj = JSON.parse(setupElectionOpenBallotJson);
      const msg = SetupElection.fromJson(obj);

      expect(msg.id).toBeInstanceOf(Hash);
      expect(msg.lao).toBeInstanceOf(Hash);
      expect(msg.created_at).toBeInstanceOf(Timestamp);
      expect(msg.start_time).toBeInstanceOf(Timestamp);
      expect(msg.end_time).toBeInstanceOf(Timestamp);

      expect(msg).toBeJsonEqual(sampleSetupElectionOpenBallot);
    });

    it('fromJson should throw an error if the Json has incorrect action', () => {
      const obj = {
        object: ObjectType.ELECTION,
        action: ActionType.NOTIFY_ADD,
        id: mockElectionId.toString(),
        lao: mockLaoIdHash.toString(),
        name: mockLaoName,
        version: OPEN_BALLOT_VERSION,
        created_at: TIMESTAMP.valueOf(),
        start_time: TIMESTAMP.valueOf(),
        end_time: CLOSE_TIMESTAMP.valueOf(),
        questions: [mockQuestionObject1, mockQuestionObject2],
      };
      const createFromJson = () => SetupElection.fromJson(obj);
      expect(createFromJson).toThrow(ProtocolError);
    });

    it('fromJson should throw an error if the Json has incorrect object', () => {
      const obj = {
        object: ObjectType.CHIRP,
        action: ActionType.SETUP,
        version: OPEN_BALLOT_VERSION,
        id: mockElectionId.toString(),
        lao: mockLaoIdHash.toString(),
        name: mockLaoName,
        created_at: TIMESTAMP.valueOf(),
        start_time: TIMESTAMP.valueOf(),
        end_time: CLOSE_TIMESTAMP.valueOf(),
        questions: [mockQuestionObject1, mockQuestionObject2],
      };
      const createFromJson = () => SetupElection.fromJson(obj);
      expect(createFromJson).toThrow(ProtocolError);
    });

    describe('constructor', () => {
      it('should throw an error if id is undefined', () => {
        const createWrongObj = () =>
          new SetupElection({
            version: OPEN_BALLOT_VERSION,
            id: undefined as unknown as Hash,
            lao: mockLaoIdHash,
            name: mockLaoName,
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
            version: OPEN_BALLOT_VERSION,
            id: mockElectionId,
            lao: undefined as unknown as Hash,
            name: mockLaoName,
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
            version: OPEN_BALLOT_VERSION,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: undefined as unknown as string,
            created_at: TIMESTAMP,
            start_time: TIMESTAMP,
            end_time: CLOSE_TIMESTAMP,
            questions: [mockQuestionObject1, mockQuestionObject2],
          });
        expect(createWrongObj).toThrow(ProtocolError);
      });

      it('should throw an error if key is defined', () => {
        const createWrongObj = () =>
          new SetupElection({
            version: OPEN_BALLOT_VERSION,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
            key: mockKeyPair.publicKey,
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
            version: undefined as unknown as string,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
            created_at: TIMESTAMP,
            start_time: TIMESTAMP,
            end_time: CLOSE_TIMESTAMP,
            questions: [mockQuestionObject1, mockQuestionObject2],
          });
        expect(createWrongObj).toThrow(ProtocolError);
      });

      it('should throw an error if version is invalid', () => {
        const createWrongObj = () =>
          new SetupElection({
            version: '1.0.0',
            id: mockElectionId,
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
            version: OPEN_BALLOT_VERSION,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
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
            version: OPEN_BALLOT_VERSION,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
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
            version: OPEN_BALLOT_VERSION,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
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
            version: OPEN_BALLOT_VERSION,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
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
            version: OPEN_BALLOT_VERSION,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
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
            version: OPEN_BALLOT_VERSION,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
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
        version: OPEN_BALLOT_VERSION,
        id: mockElectionId,
        lao: mockLaoIdHash,
        name: mockLaoName,
        created_at: TIMESTAMP,
        start_time: TIMESTAMP,
        end_time: CLOSE_TIMESTAMP,
        questions: [mockQuestionObject1, mockQuestionObject2],
      } as MessageDataProperties<SetupElection>);

      expect(msg.object).toEqual(ObjectType.ELECTION);
      expect(msg.action).toEqual(ActionType.SETUP);
    });
  });

  describe(`${SECRET_BALLOT_VERSION}`, () => {
    it('should be created correctly from Json', () => {
      expect(
        new SetupElection(sampleSetupElectionOpenBallot as MessageDataProperties<SetupElection>),
      ).toBeJsonEqual(sampleSetupElectionOpenBallot);
      const temp = {
        object: ObjectType.ELECTION,
        action: ActionType.SETUP,
        version: SECRET_BALLOT_VERSION,
        id: mockElectionId,
        lao: mockLaoIdHash,
        name: mockLaoName,
        key: mockKeyPair.publicKey,
        created_at: TIMESTAMP,
        start_time: TIMESTAMP,
        end_time: CLOSE_TIMESTAMP,
        questions: [mockQuestionObject1, mockQuestionObject2],
      } as MessageDataProperties<SetupElection>;
      expect(new SetupElection(temp)).toBeJsonEqual(temp);
    });

    it('should be parsed correctly from Json', () => {
      const obj = JSON.parse(setupElectionSecretBallotJson);
      const msg = SetupElection.fromJson(obj);

      expect(msg.id).toBeInstanceOf(Hash);
      expect(msg.lao).toBeInstanceOf(Hash);
      expect(msg.key).toBeInstanceOf(PublicKey);
      expect(msg.created_at).toBeInstanceOf(Timestamp);
      expect(msg.start_time).toBeInstanceOf(Timestamp);
      expect(msg.end_time).toBeInstanceOf(Timestamp);
      expect(msg).toBeJsonEqual(sampleSetupElectionSecretBallot);
    });

    it('fromJson should throw an error if the Json has incorrect action', () => {
      const obj = {
        object: ObjectType.ELECTION,
        action: ActionType.NOTIFY_ADD,
        version: SECRET_BALLOT_VERSION,
        id: mockElectionId.toString(),
        lao: mockLaoIdHash.toString(),
        name: mockLaoName,
        key: mockKeyPair.publicKey,
        created_at: TIMESTAMP.valueOf(),
        start_time: TIMESTAMP.valueOf(),
        end_time: CLOSE_TIMESTAMP.valueOf(),
        questions: [mockQuestionObject1, mockQuestionObject2],
      };
      const createFromJson = () => SetupElection.fromJson(obj);
      expect(createFromJson).toThrow(ProtocolError);
    });

    it('fromJson should throw an error if the Json has incorrect object', () => {
      const obj = {
        object: ObjectType.CHIRP,
        action: ActionType.SETUP,
        version: SECRET_BALLOT_VERSION,
        id: mockElectionId.toString(),
        lao: mockLaoIdHash.toString(),
        name: mockLaoName,
        key: mockKeyPair.publicKey,
        created_at: TIMESTAMP.valueOf(),
        start_time: TIMESTAMP.valueOf(),
        end_time: CLOSE_TIMESTAMP.valueOf(),
        questions: [mockQuestionObject1, mockQuestionObject2],
      };
      const createFromJson = () => SetupElection.fromJson(obj);
      expect(createFromJson).toThrow(ProtocolError);
    });

    describe('constructor', () => {
      it('should throw an error if id is undefined', () => {
        const createWrongObj = () =>
          new SetupElection({
            version: SECRET_BALLOT_VERSION,
            id: undefined as unknown as Hash,
            lao: mockLaoIdHash,
            name: mockLaoName,
            key: mockKeyPair.publicKey,
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
            version: SECRET_BALLOT_VERSION,
            id: mockElectionId,
            lao: undefined as unknown as Hash,
            name: mockLaoName,
            key: mockKeyPair.publicKey,
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
            version: SECRET_BALLOT_VERSION,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: undefined as unknown as string,
            key: mockKeyPair.publicKey,
            created_at: TIMESTAMP,
            start_time: TIMESTAMP,
            end_time: CLOSE_TIMESTAMP,
            questions: [mockQuestionObject1, mockQuestionObject2],
          });
        expect(createWrongObj).toThrow(ProtocolError);
      });

      it('should throw an error if key is defined', () => {
        const createWrongObj = () =>
          new SetupElection({
            version: SECRET_BALLOT_VERSION,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
            // key: mockKeyPair.publicKey,
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
            version: undefined as unknown as string,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
            key: mockKeyPair.publicKey,
            created_at: TIMESTAMP,
            start_time: TIMESTAMP,
            end_time: CLOSE_TIMESTAMP,
            questions: [mockQuestionObject1, mockQuestionObject2],
          });
        expect(createWrongObj).toThrow(ProtocolError);
      });

      it('should throw an error if version is invalid', () => {
        const createWrongObj = () =>
          new SetupElection({
            version: '1.0.0',
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
            key: mockKeyPair.publicKey,
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
            version: SECRET_BALLOT_VERSION,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
            key: mockKeyPair.publicKey,
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
            version: SECRET_BALLOT_VERSION,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
            key: mockKeyPair.publicKey,
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
            version: SECRET_BALLOT_VERSION,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
            key: mockKeyPair.publicKey,
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
            version: SECRET_BALLOT_VERSION,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
            key: mockKeyPair.publicKey,
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
            version: SECRET_BALLOT_VERSION,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
            key: mockKeyPair.publicKey,
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
            version: SECRET_BALLOT_VERSION,
            id: mockElectionId,
            lao: mockLaoIdHash,
            name: mockLaoName,
            key: mockKeyPair.publicKey,
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
        version: SECRET_BALLOT_VERSION,
        id: mockElectionId,
        lao: mockLaoIdHash,
        name: mockLaoName,
        key: mockKeyPair.publicKey,
        created_at: TIMESTAMP,
        start_time: TIMESTAMP,
        end_time: CLOSE_TIMESTAMP,
        questions: [mockQuestionObject1, mockQuestionObject2],
      } as MessageDataProperties<SetupElection>);

      expect(msg.object).toEqual(ObjectType.ELECTION);
      expect(msg.action).toEqual(ActionType.SETUP);
    });
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
        SetupElection.validateQuestions([wrongQuestion], mockElectionId.valueOf());
      };
      expect(wrongValidate).toThrow(ProtocolError);
    });
  });
});
