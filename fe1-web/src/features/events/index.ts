import { eventsReducer } from './reducer';

/**
 * Configures the events feature
 */
export function configure() {
  return {
    reducers: {
      ...eventsReducer,
    },
  };
}
