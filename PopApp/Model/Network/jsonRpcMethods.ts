export enum JsonRpcMethod {
    // uninitialized placeholder
    INVALID = '__INVALID_METHOD__',

    // actual values
    BROADCAST = 'message',
    PUBLISH = 'publish',
    SUBSCRIBE = 'subscribe',
    UNSUBSCRIBE = 'unsubscribe',
    CATCHUP = 'catchup',
}