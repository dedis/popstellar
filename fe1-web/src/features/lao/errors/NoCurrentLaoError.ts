/**
 * This class allows us to distinguish errors caused because of a missing current lao
 * and allows us to handle them
 */
export class NoCurrentLaoError extends Error {
  constructor(m: string) {
    super(m);

    // set prototype explicitly, needs to happen immediately after super() call
    Object.setPrototypeOf(this, NoCurrentLaoError.prototype);

    // Maintains proper stack trace for where our error was thrown, if available
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, NoCurrentLaoError);
    }
  }
}
