/**
 * This class allows us to distinguish protocol errors from other JS errors.
 * It is used exclusively when messages are created or parsed with invalid data.
 */
export class ProtocolError extends Error {
  constructor(m: string) {
    super(m);

    // set prototype explicitly, needs to happen immediately after super() call
    Object.setPrototypeOf(this, ProtocolError.prototype);

    // Maintains proper stack trace for where our error was thrown, if available
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, ProtocolError);
    }
  }
}
