import 'jest-extended';
import { RollCall, RollCallState, RollCallStatus } from '../RollCall';
import { Hash } from '../Hash';
import { Timestamp } from '../Timestamp';
import { LaoEventType } from '../LaoEvent';
import { PopToken } from '../PopToken';
import { PrivateKey } from '../PrivateKey';
import { PublicKey } from '../PublicKey';

const ID = new Hash('rollCallId');
const NAME = 'myRollCall';
const LOCATION = 'location';
const TIMESTAMP_START = new Timestamp(1620255600);
const TIMESTAMP_END = new Timestamp(1620357600);
const ATTENDEES = ['attendee1', 'attendee2'];
const token = new PopToken({
  publicKey: new PublicKey('attendee1'),
  privateKey: new PrivateKey('privateKey'),
});

describe('RollCall object', () => {
  it('can do a state round trip correctly 1', () => {
    const rollCallState: RollCallState = {
      id: ID.valueOf(),
      eventType: LaoEventType.ROLL_CALL,
      start: TIMESTAMP_START.valueOf(),
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP_START.valueOf(),
      proposed_start: TIMESTAMP_START.valueOf(),
      proposed_end: TIMESTAMP_END.valueOf(),
      status: RollCallStatus.CLOSED,
      attendees: ATTENDEES,
    };
    const expected = {
      id: ID.valueOf(),
      eventType: LaoEventType.ROLL_CALL,
      start: TIMESTAMP_START.valueOf(),
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP_START.valueOf(),
      proposed_start: TIMESTAMP_START.valueOf(),
      proposed_end: TIMESTAMP_END.valueOf(),
      status: RollCallStatus.CLOSED,
      end: TIMESTAMP_END.valueOf(),
      attendees: ATTENDEES,
    };
    const rollCall = RollCall.fromState(rollCallState);
    expect(rollCall.toState()).toStrictEqual(expected);
  });

  it('can do a state round trip correctly 2', () => {
    const rollCallState: RollCallState = {
      id: ID.valueOf(),
      idAlias: 'idAlias',
      eventType: LaoEventType.ROLL_CALL,
      start: TIMESTAMP_START.valueOf(),
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP_START.valueOf(),
      proposed_start: TIMESTAMP_START.valueOf(),
      opened_at: TIMESTAMP_START.valueOf(),
      proposed_end: TIMESTAMP_END.valueOf(),
      closed_at: TIMESTAMP_END.valueOf(),
      status: RollCallStatus.CLOSED,
      attendees: ATTENDEES,
    };
    const expected = {
      id: ID.valueOf(),
      idAlias: 'idAlias',
      eventType: LaoEventType.ROLL_CALL,
      start: TIMESTAMP_START.valueOf(),
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP_START.valueOf(),
      proposed_start: TIMESTAMP_START.valueOf(),
      proposed_end: TIMESTAMP_END.valueOf(),
      status: RollCallStatus.CLOSED,
      end: TIMESTAMP_END.valueOf(),
      attendees: ATTENDEES,
    };
    const rollCall = RollCall.fromState(rollCallState);
    expect(rollCall.toState()).toStrictEqual(expected);
  });

  it('containsToken function works when attendees is undefined', () => {
    const rollCall = new RollCall({
      id: ID,
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP_START,
      proposed_start: TIMESTAMP_START,
      proposed_end: TIMESTAMP_END,
      status: RollCallStatus.CLOSED,
    });
    expect(rollCall.containsToken(token)).toBeFalse();
  });

  it('containsToken function works when token is undefined', () => {
    const rollCall = new RollCall({
      id: ID,
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP_START,
      proposed_start: TIMESTAMP_START,
      proposed_end: TIMESTAMP_END,
      status: RollCallStatus.CLOSED,
      attendees: ATTENDEES.map((s: string) => new PublicKey(s)),
    });
    expect(rollCall.containsToken(undefined as unknown as PopToken)).toBeFalse();
  });

  it('containsToken function works when token is undefined', () => {
    const rollCall = new RollCall({
      id: ID,
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP_START,
      proposed_start: TIMESTAMP_START,
      proposed_end: TIMESTAMP_END,
      status: RollCallStatus.CLOSED,
      attendees: ATTENDEES.map((s: string) => new PublicKey(s)),
    });
    expect(rollCall.containsToken(undefined as unknown as PopToken)).toBeFalse();
  });

  it('containsToken function works when attendees and token are defined', () => {
    const rollCall = new RollCall({
      id: ID,
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP_START,
      proposed_start: TIMESTAMP_START,
      proposed_end: TIMESTAMP_END,
      status: RollCallStatus.CLOSED,
      attendees: ATTENDEES.map((s: string) => new PublicKey(s)),
    });
    expect(rollCall.containsToken(token)).toBeTrue();
  });

  describe('constructor', () => {
    it('throws an error when object is undefined', () => {
      const partial = undefined as unknown as Partial<RollCall>;
      const createWrongRollCall = () => new RollCall(partial);
      expect(createWrongRollCall).toThrow(Error);
    });

    it('throws an error when object is null', () => {
      const partial = null as unknown as Partial<RollCall>;
      const createWrongRollCall = () => new RollCall(partial);
      expect(createWrongRollCall).toThrow(Error);
    });

    it('throws an error when id is undefined', () => {
      const createWrongRollCall = () => new RollCall({
        name: NAME,
        location: LOCATION,
        creation: TIMESTAMP_START,
        proposed_start: TIMESTAMP_START,
        proposed_end: TIMESTAMP_END,
        status: RollCallStatus.CLOSED,
      });
      expect(createWrongRollCall).toThrow(Error);
    });

    it('throws an error when name is undefined', () => {
      const createWrongRollCall = () => new RollCall({
        id: ID,
        location: LOCATION,
        creation: TIMESTAMP_START,
        proposed_start: TIMESTAMP_START,
        proposed_end: TIMESTAMP_END,
        status: RollCallStatus.CLOSED,
      });
      expect(createWrongRollCall).toThrow(Error);
    });

    it('throws an error when location is undefined', () => {
      const createWrongRollCall = () => new RollCall({
        id: ID,
        start: TIMESTAMP_START,
        name: NAME,
        creation: TIMESTAMP_START,
        proposed_start: TIMESTAMP_START,
        proposed_end: TIMESTAMP_END,
        status: RollCallStatus.CLOSED,
      });
      expect(createWrongRollCall).toThrow(Error);
    });

    it('throws an error when creation is undefined', () => {
      const createWrongRollCall = () => new RollCall({
        id: ID,
        start: TIMESTAMP_START,
        name: NAME,
        location: LOCATION,
        proposed_start: TIMESTAMP_START,
        proposed_end: TIMESTAMP_END,
        status: RollCallStatus.CLOSED,
      });
      expect(createWrongRollCall).toThrow(Error);
    });

    it('throws an error when proposed_start is undefined', () => {
      const createWrongRollCall = () => new RollCall({
        id: ID,
        start: TIMESTAMP_START,
        name: NAME,
        location: LOCATION,
        creation: TIMESTAMP_START,
        proposed_end: TIMESTAMP_END,
        status: RollCallStatus.CLOSED,
      });
      expect(createWrongRollCall).toThrow(Error);
    });

    it('throws an error when proposed_end is undefined', () => {
      const createWrongRollCall = () => new RollCall({
        id: ID,
        start: TIMESTAMP_START,
        name: NAME,
        location: LOCATION,
        creation: TIMESTAMP_START,
        proposed_start: TIMESTAMP_START,
        status: RollCallStatus.CLOSED,
      });
      expect(createWrongRollCall).toThrow(Error);
    });

    it('throws an error when status is undefined', () => {
      const createWrongRollCall = () => new RollCall({
        id: ID,
        start: TIMESTAMP_START,
        name: NAME,
        location: LOCATION,
        creation: TIMESTAMP_START,
        proposed_start: TIMESTAMP_START,
        proposed_end: TIMESTAMP_END,
      });
      expect(createWrongRollCall).toThrow(Error);
    });
  });
});
