import { addReducer } from 'core/redux';
import { eventsReducer } from './reducer';

/**
 * Configures the events feature
 */
export function configure() {
  addReducer(eventsReducer);
}
