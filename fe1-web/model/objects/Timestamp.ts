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
      throw new Error('Timestamp constructor requires a number, a timestamp or its string representation.');
    }

    super(parsedValue);
  }

  public static EpochNow(): Timestamp {
    return new Timestamp(Math.floor(Date.now() / 1000));
  }

  public before(other: Timestamp): boolean {
    return this.valueOf() < other.valueOf();
  }

  public after(other: Timestamp): boolean {
    return this.valueOf() > other.valueOf();
  }
}
