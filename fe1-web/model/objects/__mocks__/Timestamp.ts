const mockEpoch = 1609455600;

export class Timestamp extends Number {
  public static EpochNow() {
    return new Timestamp(mockEpoch);
  }

  public before(other: Timestamp): boolean {
    return this.valueOf() < other.valueOf();
  }

  public after(other: Timestamp): boolean {
    return this.valueOf() > other.valueOf();
  }
}
