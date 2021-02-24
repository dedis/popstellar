export class Timestamp extends Number {
  public static EpochNow(): Timestamp {
    return new Timestamp(Math.floor(Date.now() / 1000));
  }
}
