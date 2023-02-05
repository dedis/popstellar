import 'jest-extended';

import { Hash, PopToken, PrivateKey, PublicKey, Timestamp } from 'core/objects';

import { RollCall, RollCallStatus } from '../RollCall';

const ID = new Hash('rollCallId');
const NAME = 'myRollCall';
const LOCATION = 'location';
const TIMESTAMP_START = new Timestamp(1620355600);
const TIMESTAMP_END = new Timestamp(1620357600);
const ATTENDEES = ['attendee1', 'attendee2'].sort();
const token = new PopToken({
  publicKey: new PublicKey('attendee1'),
  privateKey: new PrivateKey('privateKey'),
});

describe('RollCall object', () => {
  it('can do a state round trip correctly 1', () => {
    const rollCallState: any = {
      id: ID.valueOf(),
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP_START.valueOf(),
      proposedStart: TIMESTAMP_START.valueOf(),
      proposedEnd: TIMESTAMP_END.valueOf(),
      status: RollCallStatus.CLOSED,
      attendees: ATTENDEES,
    };
    const expected = {
      id: ID.valueOf(),
      name: NAME,
      location: LOCATION,
      closedAt: undefined,
      description: undefined,
      idAlias: undefined,
      openedAt: undefined,
      creation: TIMESTAMP_START.valueOf(),
      proposedStart: TIMESTAMP_START.valueOf(),
      proposedEnd: TIMESTAMP_END.valueOf(),
      status: RollCallStatus.CLOSED,
      attendees: ATTENDEES,
    };
    const rollCall = RollCall.fromState(rollCallState);
    expect(rollCall.toState()).toStrictEqual(expected);
  });

  it('can do a state round trip correctly 2', () => {
    const rollCallState: any = {
      id: ID.valueOf(),
      idAlias: 'idAlias',
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP_START.valueOf(),
      proposedStart: TIMESTAMP_START.valueOf(),
      openedAt: TIMESTAMP_START.valueOf(),
      proposedEnd: TIMESTAMP_END.valueOf(),
      closedAt: TIMESTAMP_END.valueOf(),
      status: RollCallStatus.CLOSED,
      attendees: ATTENDEES,
    };
    const expected = {
      id: ID.valueOf(),
      idAlias: 'idAlias',
      name: NAME,
      location: LOCATION,
      creation: TIMESTAMP_START.valueOf(),
      description: undefined,
      proposedStart: TIMESTAMP_START.valueOf(),
      openedAt: TIMESTAMP_START.valueOf(),
      proposedEnd: TIMESTAMP_END.valueOf(),
      closedAt: TIMESTAMP_END.valueOf(),
      status: RollCallStatus.CLOSED,
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
      proposedStart: TIMESTAMP_START,
      proposedEnd: TIMESTAMP_END,
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
      proposedStart: TIMESTAMP_START,
      proposedEnd: TIMESTAMP_END,
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
      proposedStart: TIMESTAMP_START,
      proposedEnd: TIMESTAMP_END,
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
      const createWrongRollCall = () =>
        new RollCall({
          name: NAME,
          location: LOCATION,
          creation: TIMESTAMP_START,
          proposedStart: TIMESTAMP_START,
          proposedEnd: TIMESTAMP_END,
          status: RollCallStatus.CLOSED,
        });
      expect(createWrongRollCall).toThrow(Error);
    });

    it('throws an error when name is undefined', () => {
      const createWrongRollCall = () =>
        new RollCall({
          id: ID,
          location: LOCATION,
          creation: TIMESTAMP_START,
          proposedStart: TIMESTAMP_START,
          proposedEnd: TIMESTAMP_END,
          status: RollCallStatus.CLOSED,
        });
      expect(createWrongRollCall).toThrow(Error);
    });

    it('throws an error when location is undefined', () => {
      const createWrongRollCall = () =>
        new RollCall({
          id: ID,
          name: NAME,
          creation: TIMESTAMP_START,
          proposedStart: TIMESTAMP_START,
          proposedEnd: TIMESTAMP_END,
          status: RollCallStatus.CLOSED,
        });
      expect(createWrongRollCall).toThrow(Error);
    });

    it('throws an error when creation is undefined', () => {
      const createWrongRollCall = () =>
        new RollCall({
          id: ID,
          name: NAME,
          location: LOCATION,
          proposedStart: TIMESTAMP_START,
          proposedEnd: TIMESTAMP_END,
          status: RollCallStatus.CLOSED,
        });
      expect(createWrongRollCall).toThrow(Error);
    });

    it('throws an error when proposed_start is undefined', () => {
      const createWrongRollCall = () =>
        new RollCall({
          id: ID,
          name: NAME,
          location: LOCATION,
          creation: TIMESTAMP_START,
          proposedEnd: TIMESTAMP_END,
          status: RollCallStatus.CLOSED,
        });
      expect(createWrongRollCall).toThrow(Error);
    });

    it('throws an error when proposed_end is undefined', () => {
      const createWrongRollCall = () =>
        new RollCall({
          id: ID,
          name: NAME,
          location: LOCATION,
          creation: TIMESTAMP_START,
          proposedStart: TIMESTAMP_START,
          status: RollCallStatus.CLOSED,
        });
      expect(createWrongRollCall).toThrow(Error);
    });

    it('throws an error when status is undefined', () => {
      const createWrongRollCall = () =>
        new RollCall({
          id: ID,
          start: TIMESTAMP_START,
          name: NAME,
          location: LOCATION,
          creation: TIMESTAMP_START,
          proposedStart: TIMESTAMP_START,
          proposedEnd: TIMESTAMP_END,
        });
      expect(createWrongRollCall).toThrow(Error);
    });

    it('throws an error when list of attendees is not sorted', () => {
      const createWrongRollCall = () =>
        new RollCall({
          id: ID,
          start: TIMESTAMP_START,
          name: NAME,
          location: LOCATION,
          creation: TIMESTAMP_START,
          proposedStart: TIMESTAMP_START,
          proposedEnd: TIMESTAMP_END,
          attendees: ATTENDEES.map((s: string) => new PublicKey(s)).sort((a, b) => (b < a ? 1 : 0)),
        });
      expect(createWrongRollCall).toThrow(Error);
    });
  });
});
