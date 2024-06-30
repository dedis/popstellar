import {
  LinkedOrganizationsInterface,
  LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER,
  LinkedOrganizationsConfiguration,
} from './interface';
import { LinkedOrganizationsLaoScreen } from './navigation/LinkedOrganizationsNavigation';
import { configureNetwork } from './network';
import { challengeReducer, linkedOrganizationsReducer } from './reducer';

/**
 * Configures the linked organizations feature
 */
export function configure(
  configuration: LinkedOrganizationsConfiguration,
): LinkedOrganizationsInterface {
  const { useCurrentLao, useCurrentLaoId, useIsLaoOrganizer, getLaoById, getRollCallById } =
    configuration;
  configureNetwork(configuration);
  return {
    identifier: LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER,
    laoScreens: [LinkedOrganizationsLaoScreen],
    reducers: {
      ...challengeReducer,
      ...linkedOrganizationsReducer,
    },
    context: {
      useCurrentLaoId: useCurrentLaoId,
      useIsLaoOrganizer: useIsLaoOrganizer,
      useCurrentLao: useCurrentLao,
      getLaoById: getLaoById,
      getRollCallById: getRollCallById,
    },
  };
}
