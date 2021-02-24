export const mockTime = 1609455600;

export class Timestamp extends Number {
  public static EpochNow() {
    return mockTime;
  }
}
