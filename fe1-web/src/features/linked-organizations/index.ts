import {
  LinkedOrganizationsInterface,
  LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER,
  LinkedOrganizationsCompositionConfiguration,
  LinkedOrganizationsCompositionInterface,
} from './interface';
import { LinkedOrganizationsLaoScreen } from './navigation/LinkedOrganizationsNavigation';
import { configureNetwork } from './network';
import { challengeReducer, linkedOrganizationsReducer } from './reducer';

/**
 * Configures the linked organizations feature
 */
export function configure(): LinkedOrganizationsInterface {
  return {
    identifier: LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER,
    laoScreens: [LinkedOrganizationsLaoScreen],
  };
}

export function compose(
  configuration: LinkedOrganizationsCompositionConfiguration,
): LinkedOrganizationsCompositionInterface {
  configureNetwork(configuration);
  return {
    identifier: LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER,
    reducers: {
      ...challengeReducer,
      ...linkedOrganizationsReducer,
    },
    context: {
      useCurrentLaoId: configuration.useCurrentLaoId,
      useIsLaoOrganizer: configuration.useIsLaoOrganizer,
      useCurrentLao: configuration.useCurrentLao,
    },
  };
}
