import 'jest-extended';

import { Timestamp } from '../Timestamp';

const NOW = 1620255600;
const TIMESTAMP = 1620455700;

describe('Timestamp object', () => {
  it('EpochNow function works', () => {
    jest.setSystemTime(NOW * 1000);
    expect(Timestamp.EpochNow().valueOf()).toStrictEqual(NOW);
  });

  it('dateToTimestamp function works', () => {
    const date = new Date(TIMESTAMP * 1000);
    expect(Timestamp.fromDate(date).valueOf()).toStrictEqual(TIMESTAMP);
  });

  it('timestampToDate function works', () => {
    const timestamp = new Timestamp(TIMESTAMP);
    expect(timestamp.toDate().valueOf()).toStrictEqual(TIMESTAMP * 1000);
  });

  it('before function works', () => {
    const timestamp1 = new Timestamp(TIMESTAMP);
    const timestamp2 = new Timestamp(TIMESTAMP + 1000);
    expect(timestamp1.before(timestamp2)).toBeTrue();
  });

  it('after function works', () => {
    const timestamp1 = new Timestamp(TIMESTAMP);
    const timestamp2 = new Timestamp(TIMESTAMP - 1000);
    expect(timestamp1.after(timestamp2)).toBeTrue();
  });

  it('addSeconds function works', () => {
    const newTimestamp = TIMESTAMP + 100;
    expect(new Timestamp(TIMESTAMP).addSeconds(100).valueOf()).toStrictEqual(newTimestamp);
  });

  describe('constructor', () => {
    it('works with a string as argument', () => {
      const timestamp = new Timestamp('12345');
      expect(timestamp.valueOf()).toStrictEqual(12345);
    });

    it('works with a Timestamp as argument', () => {
      const timestamp = new Timestamp(new Timestamp('12345'));
      expect(timestamp.valueOf()).toStrictEqual(12345);
    });

    it('works with a number as argument', () => {
      const timestamp = new Timestamp(12345);
      expect(timestamp.valueOf()).toStrictEqual(12345);
    });

    it('works with a Number as argument', () => {
      const n = Number(12345);
      const timestamp = new Timestamp(n);
      expect(timestamp.valueOf()).toStrictEqual(12345);
    });

    it('throws an error for another type', () => {
      const createWrongTimestamp = () => new Timestamp(true);
      expect(createWrongTimestamp).toThrow(Error);
    });
  });
});
