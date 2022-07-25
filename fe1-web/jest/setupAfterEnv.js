import { jest } from '@jest/globals';

const FIXED_SYSTEM_TIME = new Date(1620255600 * 1000).getTime(); // 5 May 2021

global.beforeAll(() => {
  jest.useFakeTimers('modern');
  jest.setSystemTime(FIXED_SYSTEM_TIME);
});

global.afterAll(() => {
  jest.useRealTimers();
});
