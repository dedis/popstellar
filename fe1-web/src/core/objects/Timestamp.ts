export type TimestampState = number;

/**
 * Represents a Timestamp in our system.
 */
export class Timestamp extends Number implements Number {
  public constructor(value: unknown) {
    let parsedValue: number;

    if (typeof value === 'string') {
      parsedValue = parseFloat(value);
    } else if (value instanceof Timestamp) {
      parsedValue = value.valueOf();
    } else if (typeof value === 'number') {
      parsedValue = value;
    } else if (value instanceof Number) {
      parsedValue = value.valueOf();
    } else {
      throw new Error(
        'Timestamp constructor requires a number, a timestamp or its string representation.',
      );
    }

    super(parsedValue);
  }

  /**
   * Returns the Timestamp corresponding to the current time.
   */
  public static EpochNow(): Timestamp {
    return new Timestamp(Math.floor(Date.now() / 1000));
  }

  /**
   * Creates a Timestamp from a Date.
   *
   * @param date
   */
  public static fromDate(date: Date): Timestamp {
    return new Timestamp(Math.floor(date.getTime() / 1000));
  }

  /**
   * Returns the maximum of two Timestamps.
   *
   * @param time1
   * @param time2
   */
  public static max(time1: Timestamp, time2: Timestamp): Timestamp {
    return time1.before(time2) ? time2 : time1;
  }

  /**
   * Creates a date from the current Timestamp object.
   */
  public toDate(): Date {
    return new Date(this.valueOf() * 1000);
  }

  /**
   * Converts the current Timestamp object to a human-readable string of a date.
   */
  public toDateString(): string {
    return this.toDate().toLocaleString();
  }

  /**
   * Checks if the current Timestamp is before another Timestamp.
   *
   * @param other - The other Timestamp
   */
  public before(other: Timestamp): boolean {
    return this.valueOf() < other.valueOf();
  }

  /**
   * Checks if the current Timestamp is after another Timestamp.
   *
   * @param other - The other Timestamp
   */
  public after(other: Timestamp): boolean {
    return this.valueOf() > other.valueOf();
  }

  /**
   * Adds seconds to a Timestamp.
   *
   * @param seconds - The number of seconds to add
   */
  public addSeconds(seconds: number): Timestamp {
    return new Timestamp(this.valueOf() + seconds);
  }

  /**
   * Returns the ISO 8601 representation of the timestamp
   * If you need access to the unterlying data type use .valueOf() and
   * if you want to serialize an instance use .toState() instead
   */
  public toString(): string {
    return this.toDate().toISOString();
  }

  /**
   * Returns the primitive value used for representing the Timestamp,
   * the number of seconds that passed since 00:00:00 UTC
   * If you want to serialize an instance use .toState() instead
   */
  public valueOf(): number {
    return super.valueOf();
  }

  /**
   * Returns the serialized version of the base64url that can for instance be stored
   * in redux stores
   */
  public toState(): TimestampState {
    return super.valueOf();
  }

  /**
   * Deserializes a previously serializes instance of Base64Url
   */
  public static fromState(timestampState: TimestampState): Timestamp {
    return new Timestamp(timestampState);
  }
}
