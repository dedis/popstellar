import { mockChannel, mockLaoId } from '__tests__/utils';

import { subscribeToChannel, unsubscribeFromChannel } from '../CommunicationApi';
import { catchup, subscribe, unsubscribe } from '../JsonRpcApi';
import { NetworkConnection } from '../NetworkConnection';

jest.mock('core/network/JsonRpcApi', () => {
  return {
    ...jest.requireActual('core/network/JsonRpcApi'),
    publish: jest.fn(Promise.resolve),
    subscribe: jest.fn(Promise.resolve),
    unsubscribe: jest.fn(Promise.resolve),
    catchup: jest.fn(() => Promise.resolve([])),
  };
});

beforeEach(() => {
  jest.clearAllMocks();
});

const dispatch = jest.fn();

describe('CommunicationApi', () => {
  describe('subscribeToChannel', () => {
    it('should throw an error if the channel is invalid', async () => {
      await expect(
        subscribeToChannel(mockLaoId, dispatch, undefined as unknown as string),
      ).rejects.toBeInstanceOf(Error);
    });

    it('should throw an error if subscribe() fails', async () => {
      (subscribe as jest.Mock).mockImplementationOnce(() => Promise.reject(new Error()));
      await expect(subscribeToChannel(mockLaoId, dispatch, mockChannel)).rejects.toBeInstanceOf(
        Error,
      );
    });

    it('should throw an error if catchup() fails', async () => {
      (catchup as jest.Mock).mockImplementationOnce(() => Promise.reject(new Error()));
      await expect(subscribeToChannel(mockLaoId, dispatch, mockChannel)).rejects.toBeInstanceOf(
        Error,
      );
    });

    it('should call subscribe and catchup', async () => {
      const connections = ['some mock connection'] as unknown as NetworkConnection[];

      await expect(subscribeToChannel(mockLaoId, dispatch, mockChannel, connections)).toResolve();
      expect(subscribe).toHaveBeenCalledWith(mockChannel, connections);
      expect(subscribe).toHaveBeenCalledTimes(1);

      expect(catchup).toHaveBeenCalledWith(mockChannel, connections);
      expect(catchup).toHaveBeenCalledTimes(1);
    });

    it('should call subscribe but no catchup if specified so', async () => {
      const connections = ['some mock connection'] as unknown as NetworkConnection[];

      await expect(
        subscribeToChannel(mockLaoId, dispatch, mockChannel, connections, false),
      ).toResolve();
      expect(subscribe).toHaveBeenCalledWith(mockChannel, connections);
      expect(subscribe).toHaveBeenCalledTimes(1);

      expect(catchup).not.toHaveBeenCalled();
    });
  });

  describe('unsubscribeFromChannel', () => {
    it('should throw an error if the channel is invalid', async () => {
      await expect(
        unsubscribeFromChannel(mockLaoId, dispatch, undefined as unknown as string),
      ).rejects.toBeInstanceOf(Error);
    });

    it('should throw an error if unsubscribe() fails', async () => {
      (unsubscribe as jest.Mock).mockImplementationOnce(() => Promise.reject(new Error()));
      await expect(unsubscribeFromChannel(mockLaoId, dispatch, mockChannel)).rejects.toBeInstanceOf(
        Error,
      );
    });

    it('should call unsubscribe', async () => {
      const connections = ['some mock connection'] as unknown as NetworkConnection[];

      await expect(
        unsubscribeFromChannel(mockLaoId, dispatch, mockChannel, connections),
      ).toResolve();
      expect(unsubscribe).toHaveBeenCalledWith(mockChannel, connections);
      expect(unsubscribe).toHaveBeenCalledTimes(1);
    });
  });
});
