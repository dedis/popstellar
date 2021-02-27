export class Timestamp extends Number {
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
