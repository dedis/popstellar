import { ListItem } from '@rneui/themed';
import React, { useState } from 'react';
import { Text, View, StyleSheet } from 'react-native';

import { PoPIcon} from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';
import { LinkedOrganization } from '../objects/LinkedOrganization';

const initialOrganizations: LinkedOrganization[] = [];

const styles = StyleSheet.create({
  flex1: {
    flex: 1,
  }
});

const LinkedOrganizationsScreen = () => {
  const [organizations, setOrganizations] = useState<LinkedOrganization[]>(initialOrganizations);

  return (
    <View style={styles.flex1}>
      <ScreenWrapper>
        <Text style={Typography.paragraph}>{STRINGS.linked_organizations_description}</Text>
        <View style={List.container}>
          {organizations.map((organization, idx) => {
            const listStyle = List.getListItemStyles(
              true && idx === 0,
              idx === organizations.length - 1,
            );

            return (
              <ListItem
                containerStyle={listStyle}
                style={listStyle}
                bottomDivider
                key={organization.lao_id.valueOf()}>
                <PoPIcon name="business" />
                <ListItem.Content>
                  <ListItem.Title>
                    {STRINGS.linked_organizations_LaoID} {organization.lao_id}
                  </ListItem.Title>
                </ListItem.Content>
              </ListItem>
            );
          })}
        </View>
      </ScreenWrapper>
    </View>
  );
};

export default LinkedOrganizationsScreen;
