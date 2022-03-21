import { MessageRegistry } from 'core/network/jsonrpc/messages';

import { MeetingEventTypeComponent } from './components';
import { configureNetwork } from './network';
import * as screens from './screens';

/**
 * Configures the meeting feature
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configure(registry: MessageRegistry) {
  configureNetwork(registry);

  return {
    eventTypeComponents: [MeetingEventTypeComponent],
    screens,
  };
}
