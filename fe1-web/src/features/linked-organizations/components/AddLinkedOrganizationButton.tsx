import { useActionSheet } from '@expo/react-native-action-sheet';
import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React from 'react';

import { PoPIcon } from 'core/components';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Color, Icon } from 'core/styles';
import STRINGS from 'resources/strings';

import { LinkedOrganizationsHooks } from '../hooks';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_linked_organizations>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

const AddLinkedOrganizationButton = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();

  const { showActionSheetWithOptions } = useActionSheet();
  const laoId = LinkedOrganizationsHooks.useCurrentLaoId();
  const isOrganizer = LinkedOrganizationsHooks.useIsLaoOrganizer(laoId);

  const onPress = () => {
    showActionSheetWithOptions(
      {
        options: ['Add Linked Organization', 'cancel'],
        cancelButtonIndex: 1,
      },
      (idx) => {
        if (idx !== undefined && idx === 0) {
          navigation.navigate(STRINGS.navigation_app_lao, {
            screen: STRINGS.navigation_linked_organizations_title,
            params: {
              screen: STRINGS.linked_organizations_navigation_addlinkedorgModal,
            },
          });
        } else {
          // cancel
        }
      },
    );
  };

  return isOrganizer ? (
    <PoPTouchableOpacity onPress={onPress} testID="add_linked_organization_selector">
      <PoPIcon name="addLinkedOrg" color={Color.inactive} size={Icon.size} />
    </PoPTouchableOpacity>
  ) : null;
};

export default AddLinkedOrganizationButton;
