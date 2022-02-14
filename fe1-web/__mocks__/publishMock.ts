import { publish } from 'network/JsonRpcApi';

/**
 * Mocks the publish method that publish messages.
 */
jest.mock('network/JsonRpcApi.ts', () => ({
  publish: jest.fn(() => Promise.resolve()),
}));
export const publishMock = publish as jest.MockedFunction<typeof publish>;
