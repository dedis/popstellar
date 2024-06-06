import { ListItem } from '@rneui/themed';
import React, { useEffect, useMemo } from 'react';
import { Text, View, StyleSheet, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

import { PoPIcon } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { dispatch } from 'core/redux';
import { List, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { LinkedOrganizationsHooks } from '../hooks';
import { makeChallengeReceveidSelector, removeReceivedChallenge } from '../reducer';
import {
  addLinkedOrganization,
  makeLinkedOrganizationSelector,
  makeScannedLinkedOrganizationSelector,
  removeScannedLinkedOrganization,
} from '../reducer/LinkedOrganizationsReducer';

const styles = StyleSheet.create({
  flexibleView: {
    flex: 1,
  } as ViewStyle,
});

const LinkedOrganizationsScreen = () => {
  const laoId = LinkedOrganizationsHooks.useCurrentLaoId();
  const toast = useToast();
  const isOrganizer = LinkedOrganizationsHooks.useIsLaoOrganizer(laoId);
  const linkedOrganizationSelector = useMemo(() => makeLinkedOrganizationSelector(laoId), [laoId]);
  const linkedOrganizationStates = useSelector(linkedOrganizationSelector);

  const recvChallengeSelector = useMemo(() => makeChallengeReceveidSelector(laoId), [laoId]);
  const recvChallengeState = useSelector(recvChallengeSelector);

  const scannedLinkedOrgSelector = useMemo(
    () => makeScannedLinkedOrganizationSelector(laoId),
    [laoId],
  );
  const scannedLinkedOrgStates = useSelector(scannedLinkedOrgSelector);

  useEffect(() => {
    if (
      recvChallengeState &&
      scannedLinkedOrgStates &&
      recvChallengeState.length !== 0 &&
      scannedLinkedOrgStates.length !== 0
    ) {
      try {
        for (const [challenge, publicKey] of recvChallengeState) {
          const chall2 = challenge.toState();
          const matchingOrg = scannedLinkedOrgStates.find(
            (org) =>
              org.challenge!.value === chall2.value &&
              org.challenge!.valid_until === chall2.valid_until,
          );
          if (matchingOrg && publicKey) {
            dispatch(addLinkedOrganization(laoId, matchingOrg!));
            toast.show(`LAO linked successfully`, {
              type: 'success',
              placement: 'bottom',
              duration: FOUR_SECONDS,
            });
            dispatch(removeScannedLinkedOrganization(laoId, matchingOrg.lao_id));
            dispatch(removeReceivedChallenge(laoId, challenge, publicKey));
          } else {
            toast.show(`Could not link organizations`, {
              type: 'danger',
              placement: 'bottom',
              duration: FOUR_SECONDS,
            });
          }
        }
      } catch (e) {
        console.log(e);
      }
    }
  }, [recvChallengeState, laoId, toast, scannedLinkedOrgStates]);

  return (
    <View style={styles.flexibleView}>
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
