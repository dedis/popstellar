import { ListItem } from '@rneui/themed';
import React, { useMemo } from 'react';
import { Text, View, StyleSheet } from 'react-native';
import { useSelector } from 'react-redux';

import { PoPIcon } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { LinkedOrganizationsHooks } from '../hooks';
import { makeLinkedOrganizationSelector } from '../reducer/LinkedOrganizationsReducer';

const styles = StyleSheet.create({
  flex1: {
    flex: 1,
  },
});

const LinkedOrganizationsScreen = () => {
  const laoId = LinkedOrganizationsHooks.useCurrentLaoId();
  const isOrganizer = LinkedOrganizationsHooks.useIsLaoOrganizer(laoId);
  const linkedOrganizationSelector = useMemo(() => makeLinkedOrganizationSelector(laoId), [laoId]);
  const linkedOrganizationStates = useSelector(linkedOrganizationSelector);

  return (
    <View style={styles.flex1}>
      <ScreenWrapper>
        <Text style={Typography.paragraph}>
          {isOrganizer
            ? STRINGS.linked_organizations_description_organizer
            : STRINGS.linked_organizations_description}
        </Text>
        <View style={List.container}>
          {linkedOrganizationStates.map((linkedOrgState) => (
            <ListItem bottomDivider key={linkedOrgState.lao_id.valueOf()}>
              <PoPIcon name="business" />
              <ListItem.Content>
                <ListItem.Title>
                  {STRINGS.linked_organizations_LaoID} {linkedOrgState.lao_id.valueOf()}
                </ListItem.Title>
              </ListItem.Content>
            </ListItem>
          ))}
        </View>
      </ScreenWrapper>
    </View>
  );
};

export default LinkedOrganizationsScreen;
