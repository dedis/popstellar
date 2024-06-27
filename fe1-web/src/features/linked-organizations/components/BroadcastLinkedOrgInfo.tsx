import React, { useEffect } from 'react';
import { Hash } from 'core/objects';
import { LinkedOrganizationsHooks } from '../hooks';
import { catchup, subscribeToChannel } from 'core/network';
import { dispatch } from 'core/redux';
import { useToast } from 'react-native-toast-notifications';
import { FOUR_SECONDS } from 'resources/const';

interface BroadcastLinkedOrgInfoProps {
  linkedLaoId: Hash,
}



const BroadcastLinkedOrgInfo: React.FC<BroadcastLinkedOrgInfoProps> = ({
  linkedLaoId
}) => {
  const laoId = LinkedOrganizationsHooks.useCurrentLaoId();
  console.log(LinkedOrganizationsHooks.useGetLaoById(laoId))
  const isOrganizer = LinkedOrganizationsHooks.useIsLaoOrganizer(laoId);
  const toast = useToast();
  const fetchedLao = LinkedOrganizationsHooks.useGetLaoById(linkedLaoId);
  if (fetchedLao?.last_roll_call_id === undefined) {
    toast.show(`Data Exchange failed: Linked Organization has no last roll call id`, {
      type: 'danger',
      placement: 'bottom',
      duration: FOUR_SECONDS,
    });
    return (null);
  }
  const fetchedRollCall = LinkedOrganizationsHooks.useGetRollCallById(fetchedLao.last_roll_call_id);
  if (fetchedRollCall === undefined) {
    toast.show(`Data Exchange failed: RollCall ID is undefined`, {
      type: 'danger',
      placement: 'bottom',
      duration: FOUR_SECONDS,
    });
    return (null);
  }
  console.log(fetchedRollCall.attendees)
  return (null);
};

export default BroadcastLinkedOrgInfo;
