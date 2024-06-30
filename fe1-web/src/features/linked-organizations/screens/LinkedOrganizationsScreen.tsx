import { ListItem } from '@rneui/themed';
import React, { useEffect, useMemo, useState } from 'react';
import { Text, View, StyleSheet, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

import { PoPIcon } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { catchup, subscribeToChannel } from 'core/network';
import { channelFromIds, Hash } from 'core/objects';
import { dispatch } from 'core/redux';
import { List, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import BroadcastLinkedOrgInfo from '../components/BroadcastLinkedOrgInfo';
import { LinkedOrganizationsHooks } from '../hooks';
import { LinkedOrganization } from '../objects/LinkedOrganization';
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
  const linkedOrganizationIds = useSelector(linkedOrganizationSelector);

  const recvChallengeSelector = useMemo(() => makeChallengeReceveidSelector(laoId), [laoId]);
  const recvChallengeState = useSelector(recvChallengeSelector);

  const scannedLinkedOrgSelector = useMemo(
    () => makeScannedLinkedOrganizationSelector(laoId),
    [laoId],
  );
  const scannedLinkedOrgStates = useSelector(scannedLinkedOrgSelector);
  const [linkedLaoId, setLinkedLaoId] = useState<Hash | null>(null);

  useEffect(() => {
    const fetchData = async (linkedOrgId: Hash) => {
      const channel = channelFromIds(linkedOrgId);
      let counter = 0;
      const subscribeAndCatchup = async (): Promise<void> => {
        try {
          await subscribeToChannel(linkedOrgId, dispatch, channel);
          await catchup(channel);
          await new Promise((resolve) => setTimeout(resolve, 2000));
          setLinkedLaoId(linkedOrgId);
        } catch (error) {
          console.log(error);
          if (counter >= 10) {
            return;
          }
          counter += 1;
          await new Promise((resolve) => setTimeout(resolve, 5000)); // wait before retrying
          await subscribeAndCatchup(); // retry by calling the function recursively
        }
      };
      await subscribeAndCatchup();
    };
    if (
      recvChallengeState &&
      scannedLinkedOrgStates &&
      recvChallengeState.length !== 0 &&
      scannedLinkedOrgStates.length !== 0 &&
      isOrganizer
    ) {
      try {
        for (const [challenge, publicKey] of recvChallengeState) {
          const matchingOrg = scannedLinkedOrgStates.find(
            (org) =>
              org.challenge!.value.valueOf() === challenge.value.valueOf() &&
              org.challenge!.valid_until.valueOf() === challenge.valid_until.valueOf(),
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
            const linkedOrg = LinkedOrganization.fromState(matchingOrg);
            fetchData(linkedOrg.lao_id);
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
  }, [recvChallengeState, laoId, linkedLaoId, toast, scannedLinkedOrgStates, isOrganizer]);

  return (
    <View style={styles.flexibleView}>
      <ScreenWrapper>
        <Text style={Typography.paragraph}>
          {isOrganizer
            ? STRINGS.linked_organizations_description_organizer
            : STRINGS.linked_organizations_description}
        </Text>
        <View style={List.container}>
          {linkedOrganizationIds.map((id) => (
            <ListItem bottomDivider key={id}>
              <PoPIcon name="business" />
              <ListItem.Content>
                <ListItem.Title>
                  {STRINGS.linked_organizations_LaoID} {id}
                </ListItem.Title>
              </ListItem.Content>
            </ListItem>
          ))}
        </View>
        {linkedLaoId && <BroadcastLinkedOrgInfo linkedLaoId={linkedLaoId} />}
      </ScreenWrapper>
    </View>
  );
};

export default LinkedOrganizationsScreen;
