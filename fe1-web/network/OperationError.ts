/**
 * This class allows us to distinguish operation errors
 * (server sending back and error) from other JS errors.
 */
export class OperationError extends Error {
  public readonly errorCode: number;

  constructor(m: string, errorCode: number) {
    super(m);

    // set prototype explicitly, needs to happen immediately after super() call
    Object.setPrototypeOf(this, OperationError.prototype);

    // Maintains proper stack trace for where our error was thrown, if available
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, OperationError);
    }

    this.errorCode = errorCode;
  }
}
