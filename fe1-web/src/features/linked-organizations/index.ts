import {
  LinkedOrganizationsInterface,
  LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER,
  LinkedOrganizationsCompositionConfiguration,
  LinkedOrganizationsCompositionInterface,
} from './interface';
import {  LinkedOrganizationsLaoScreen } from './navigation/LinkedOrganizationsNavigation';
import { linkedOrganizationsReducer } from './reducer';

/**
 * Configures the wallet feature
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
  return {
    identifier: LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER,
    reducers: {
      ...linkedOrganizationsReducer,
    },
    context: {
      useCurrentLaoId: configuration.useCurrentLaoId,
      useConnectedToLao: configuration.useConnectedToLao,
      useIsLaoOrganizer: configuration.useIsLaoOrganizer,
    },
  };
}
