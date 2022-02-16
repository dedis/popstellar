import 'jest-extended';
import STRINGS from 'res/strings';
import { Meeting, MeetingState } from 'features/meeting/objects';
import {
  Election, ElectionState, Question, RegisteredVote,
} from 'features/evoting/objects';
import { RollCall, RollCallState, RollCallStatus } from 'features/rollCall/objects';
import { LaoEventState, LaoEventType } from '../LaoEvent';
import { eventFromState } from '../LaoEventBuilder';

describe('LaoEventBuilder', () => {
  it('builds a meeting with a MeetingState', () => {
    const meeting: MeetingState = {
      eventType: LaoEventType.MEETING,
      id: 'meetingId',
      start: 12345,
      name: 'myMeeting',
      location: 'location',
      creation: 12345,
      lastModified: 12345,
      extra: {},
    };
    expect(eventFromState(meeting)).toBeInstanceOf(Meeting);
  });

  it('builds a roll call with a RollCallState', () => {
    const rollCall: RollCallState = {
      eventType: LaoEventType.ROLL_CALL,
      id: 'rollCallId',
      start: 12345,
      name: 'myRollCall',
      location: 'location',
      creation: 12345,
      proposed_start: 12345,
      proposed_end: 14345,
      status: RollCallStatus.CLOSED,
    };
    expect(eventFromState(rollCall)).toBeInstanceOf(RollCall);
  });

  it('builds an election with an ElectionState', () => {
    const question1: Question = {
      id: 'q1',
      question: 'Question1',
      voting_method: STRINGS.election_method_Plurality,
      ballot_options: ['Answer1.1', 'Answer1.2', 'Answer 1.3'],
      write_in: false,
    };
    const registeredVotes: RegisteredVote = {
      createdAt: 1520255700,
      sender: 'Sender1',
      votes: [{ id: 'v1', question: 'q1' }],
      messageId: 'messageId1',
    };
    const election: ElectionState = {
      eventType: LaoEventType.ELECTION,
      id: 'electionId',
      lao: 'MyLao',
      name: 'MyElection',
      version: 'version',
      createdAt: 12345,
      start: 12345,
      end: 16345,
      questions: [question1],
      registeredVotes: [registeredVotes],
    };
    expect(eventFromState(election)).toBeInstanceOf(Election);
  });

  it('returns undefined for an event type that does not exist', () => {
    const eventState: LaoEventState = {
      eventType: '' as LaoEventType,
      id: 'eventId',
      start: 12345,
    };
    expect(eventFromState(eventState)).toBeUndefined();
  });
});
