/**
 * This class allows us to distinguish network errors from other JS errors.
 */
export class NetworkError extends Error {
  constructor(m: string) {
    super(m);

    // set prototype explicitly, needs to happen immediately after super() call
    Object.setPrototypeOf(this, NetworkError.prototype);

    // Maintains proper stack trace for where our error was thrown, if available
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, NetworkError);
    }
  }
}
