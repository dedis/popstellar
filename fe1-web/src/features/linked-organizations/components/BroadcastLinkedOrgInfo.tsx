import React from 'react';
import { useToast } from 'react-native-toast-notifications';

import { Hash } from 'core/objects';
import { FOUR_SECONDS } from 'resources/const';

import { LinkedOrganizationsHooks } from '../hooks';
import { tokensExchange } from '../network';

interface BroadcastLinkedOrgInfoProps {
  linkedLaoId: Hash;
}

const BroadcastLinkedOrgInfo: React.FC<BroadcastLinkedOrgInfoProps> = ({ linkedLaoId }) => {
  const laoId = LinkedOrganizationsHooks.useCurrentLaoId();
  const toast = useToast();
  const fetchedLao = LinkedOrganizationsHooks.useGetLaoById(linkedLaoId);
  const fetchedRollCall = LinkedOrganizationsHooks.useGetRollCallById(
    fetchedLao!.last_roll_call_id!,
  );

  if (fetchedRollCall === undefined) {
    toast.show(`Data Exchange failed: RollCall ID is undefined`, {
      type: 'danger',
      placement: 'bottom',
      duration: FOUR_SECONDS,
    });
    return null;
  }
  if (fetchedRollCall.attendees === undefined) {
    toast.show(`Data Exchange failed: RollCall Attendees is undefined`, {
      type: 'danger',
      placement: 'bottom',
      duration: FOUR_SECONDS,
    });
    return null;
  }
  tokensExchange(laoId, linkedLaoId, fetchedRollCall.id, fetchedRollCall.attendees)
    .then(() => {
      console.log('Data Exchange successfull');
    })
    .catch((err) => {
      console.log(err);
      toast.show(`Could not exchange Tokens, error: ${err}`, {
        type: 'danger',
        placement: 'bottom',
        duration: FOUR_SECONDS,
      });
    });
  return null;
};

export default BroadcastLinkedOrgInfo;
