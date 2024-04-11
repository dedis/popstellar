import {
  LinkedOrganizationsInterface,
  LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER,
  LinkedOrganizationsCompositionConfiguration,
  LinkedOrganizationsCompositionInterface,
} from './interface';
import {  LinkedOrganizationsLaoScreen } from './navigation/LinkedOrganizationsNavigation';

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
    reducers: {},
    context: {
      useCurrentLaoId: configuration.useCurrentLaoId,
      useIsLaoOrganizer: configuration.useIsLaoOrganizer,
    },
  };
}
