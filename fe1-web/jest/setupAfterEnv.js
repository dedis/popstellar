import FakeTimers from '@sinonjs/fake-timers';

/**
 * Sets up fake timers for tests.
 *
 * @remarks
 * We use a library because there is an issue between jest.useFakeTimers and asynchronous tests.
 * (see https://github.com/facebook/jest/issues/10221)
 */

const FIXED_SYSTEM_TIME = new Date(1620255600 * 1000); // 5 May 2021

let clock;
global.beforeAll(() => {
  clock = FakeTimers.install({
    now: FIXED_SYSTEM_TIME,
    shouldAdvanceTime: true,
    toFake: ['Date'],
  });
});

global.afterAll(() => {
  clock.uninstall();
});
