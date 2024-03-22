import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import { ListItem, FAB } from '@rneui/themed';
import React, { useMemo, useState } from 'react';
import { Text, View } from 'react-native';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LinkedOrganizationsParamList } from 'core/navigation/typing/LinkedOrganizationsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Hash } from 'core/objects';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';
import { LinkedOrganizationsHooks } from '../hooks';
import { Organization } from '../objects/Organizations';
import { accent, contrast, primary } from 'core/styles/color';
import { PoPIcon } from 'core/components';


type NavigationProps = CompositeScreenProps<
  StackScreenProps<LinkedOrganizationsParamList, typeof STRINGS.navigation_linked_organizations>,
  CompositeScreenProps<
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>,
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_linked_organizations>
  >
>;


const initialOrganizations: Organization[] = [
  Organization.fromState({ id: 1, name: 'Linked Org 1' }),
  Organization.fromState({ id: 2, name: 'Linked Org 2' }),
  Organization.fromState({ id: 3, name: 'Linked Org 3' }),
  // Add your initial organization items here
];

const LinkedOrganizationsScreen = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  //const laoId = LinkedOrganizationsHooks.useCurrentLaoId();

  //const isOrganizer = LinkedOrganizationsHooks.useIsLaoOrganizer(laoId);

  const [organizations, setOrganizations] = useState<Organization[]>(initialOrganizations);

  const handleAdd = () => {
    // Logic for adding a new organization
  };


  return (
    <View style={{flex: 1}}>
    <ScreenWrapper>
      <Text style={Typography.paragraph}>{STRINGS.linked_organizations_description}</Text>
      <View style={List.container}>
        {organizations.map((organization, idx) => {
          const listStyle = List.getListItemStyles(
            true && idx === 0,
            idx === organizations.length-1,
          );

          return (
            <ListItem key={organization.id}
            containerStyle={listStyle}
            style={listStyle}
            bottomDivider>
              <PoPIcon name="business"/>
              <ListItem.Content>
                <ListItem.Title>{organization.name}</ListItem.Title>
                <ListItem.Subtitle>ID: {organization.id}</ListItem.Subtitle>
              </ListItem.Content>
              <ListItem.Chevron />
            </ListItem>
          );})}
      </View>
    </ScreenWrapper>
    <FAB 
        placement="right"
        color={accent}
        onPress={handleAdd}
        icon={{ name: 'add', color: contrast }}
        style={{position: 'absolute', bottom: 30, right: 30}}
      />
    </View>
  );
};

export default LinkedOrganizationsScreen;
