import {
  Hash,
  HashState,
  PopToken,
  PublicKey,
  PublicKeyState,
  Timestamp,
  TimestampState,
} from 'core/objects';

/**
 * Object to represent a roll call.
 */

export enum RollCallStatus {
  CREATED,
  OPENED,
  CLOSED,
  REOPENED,
}

export interface RollCallState {
  id: HashState;
  idAlias?: HashState;
  name: string;
  location: string;
  description?: string;
  creation: TimestampState;
  proposedStart: TimestampState;
  proposedEnd: TimestampState;
  openedAt?: TimestampState;
  closedAt?: TimestampState;
  status: number;
  attendees?: PublicKeyState[];
}

export class RollCall {
  public static EVENT_TYPE = 'ROLL_CALL';

  public readonly id: Hash;

  public readonly idAlias?: Hash;

  public readonly name: string;

  public readonly location: string;

  public readonly description?: string;

  public readonly creation: Timestamp;

  public readonly proposedStart: Timestamp;

  public readonly proposedEnd: Timestamp;

  public readonly openedAt?: Timestamp;

  public readonly closedAt?: Timestamp;

  public readonly status: RollCallStatus;

  public readonly attendees?: PublicKey[];

  public get start() {
    return this.openedAt || this.proposedStart;
  }

  public get end() {
    return this.closedAt;
  }

  /* Not yet implemented:
   * This object should probably also keep a list of the time periods
   * during which the roll call was open (or re-opened).
   */

  constructor(obj: Partial<RollCall>) {
    if (obj === undefined || obj === null) {
      throw new Error(
        'Error encountered while creating a RollCall object: undefined/null parameters',
      );
    }

    if (obj.id === undefined) {
      throw new Error("Undefined 'id' when creating 'RollCall'");
    }
    if (obj.name === undefined) {
      throw new Error("Undefined 'name' when creating 'RollCall'");
    }
    if (obj.location === undefined) {
      throw new Error("Undefined 'location' when creating 'RollCall'");
    }
    if (obj.creation === undefined) {
      throw new Error("Undefined 'creation' when creating 'RollCall'");
    }
    if (obj.proposedStart === undefined) {
      throw new Error("Undefined 'proposed_start' when creating 'RollCall'");
    }
    if (obj.proposedEnd === undefined) {
      throw new Error("Undefined 'proposed_end' when creating 'RollCall'");
    }
    if (obj.status === undefined) {
      throw new Error("Undefined 'status' when creating 'RollCall'");
    }

    this.id = obj.id;
    this.idAlias = obj.idAlias;
    this.name = obj.name;
    this.location = obj.location;
    this.description = obj.description;
    this.creation = obj.creation;
    this.proposedStart = obj.proposedStart;
    this.proposedEnd = obj.proposedEnd;
    this.status = obj.status;
    this.attendees = obj.attendees;

    this.openedAt = obj.openedAt;
    this.closedAt = obj.closedAt;
  }

  /**
   * Checks if a pop token is contained in the current roll call.
   *
   * @param token - The pop token to search for
   * @returns A boolean telling if the token is there or not
   */
  public containsToken(token: PopToken | undefined): boolean {
    if (this.attendees === undefined || token === undefined) {
      return false;
    }

    return this.attendees.some((attendee: PublicKey) => attendee.equals(token.publicKey));
  }

  /**
   * Creates a RollCallState object from the current RollCall object.
   */
  public toState(): RollCallState {
    return {
      id: this.id.toState(),
      idAlias: this.idAlias?.toState(),
      name: this.name,
      location: this.location,
      description: this.description,
      creation: this.creation.toState(),
      proposedStart: this.proposedStart.toState(),
      proposedEnd: this.proposedEnd.toState(),
      openedAt: this.openedAt?.toState(),
      closedAt: this.closedAt?.toState(),
      status: this.status,
      attendees: this.attendees?.map((attendee) => attendee.toState()),
    };
  }

  /**
   * Creates a RollCall object from a RollCallState object.
   */
  public static fromState(rollCallState: RollCallState): RollCall {
    return new RollCall({
      id: Hash.fromState(rollCallState.id),
      idAlias: rollCallState.idAlias ? Hash.fromState(rollCallState.idAlias) : undefined,
      name: rollCallState.name,
      location: rollCallState.location,
      description: rollCallState.description,
      creation: Timestamp.fromState(rollCallState.creation),
      proposedStart: Timestamp.fromState(rollCallState.proposedStart),
      proposedEnd: Timestamp.fromState(rollCallState.proposedEnd),
      openedAt:
        rollCallState.openedAt !== undefined
          ? Timestamp.fromState(rollCallState.openedAt)
          : undefined,
      closedAt:
        rollCallState.closedAt !== undefined
          ? Timestamp.fromState(rollCallState.closedAt)
          : undefined,
      status: rollCallState.status,
      attendees: rollCallState.attendees?.map((a) => PublicKey.fromState(a)),
    });
  }
}
