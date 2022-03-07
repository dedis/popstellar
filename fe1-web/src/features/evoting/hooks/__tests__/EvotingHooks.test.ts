import { describe } from '@jest/globals';
import { configure } from 'features/evoting';
import { mockLao, mockLaoIdHash, mockMessageRegistry, mockReduxAction } from '__tests__/utils';
import { EvotingHooks } from '../index';

const onConfirmEventCreation = jest.fn();

beforeAll(() => {
  configure({
    getCurrentLao: () => mockLao,
    getCurrentLaoId: () => mockLaoIdHash,
    addEvent: () => mockReduxAction,
    updateEvent: () => mockReduxAction,
    getEventFromId: () => undefined,
    messageRegistry: mockMessageRegistry,
    onConfirmEventCreation,
  });
});

describe('E-Voting hooks', () => {
  describe('EvotingHooks.useCurrentLao', () => {
    it('should return the current lao', () => {
      expect(EvotingHooks.useCurrentLao()).toEqual(mockLao);
    });
  });

  describe('EvotingHooks.useCurrentLaoId', () => {
    it('should return the current lao id', () => {
      expect(EvotingHooks.useCurrentLaoId()).toEqual(mockLaoIdHash);
    });
  });

  describe('EvotingHooks.useOnConfirmEventCreation', () => {
    it('should return the onConfirmEventCreation config option', () => {
      expect(EvotingHooks.useOnConfirmEventCreation()).toEqual(onConfirmEventCreation);
    });
  });
});
