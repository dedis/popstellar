/**
 * RpcOperationError is the class of errors raised by RPC operations.
 * It allows the application to distinguish RPC errors from other JS errors.
 */
export class RpcOperationError extends Error {
  // Error code returned by the JSON-RPC layer
  public readonly errorCode: number;

  // Arbitrary, complex data returned as part of the error
  public readonly data?: any;

  constructor(m: string, errorCode: number, data: any = undefined) {
    super(m);

    // set prototype explicitly, needs to happen immediately after super() call
    Object.setPrototypeOf(this, RpcOperationError.prototype);

    // Maintains proper stack trace for where our error was thrown, if available
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, RpcOperationError);
    }

    this.errorCode = errorCode;
    this.data = data;
  }
}
