import { jest } from '@jest/globals';

const FIXED_SYSTEM_TIME = new Date(1620255600 * 1000); // 5 May 2021

global.beforeAll(() => {
  jest.useFakeTimers({ advanceTimers: true, now: FIXED_SYSTEM_TIME });
});
