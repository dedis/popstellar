import * as React from 'react';
import { ListRenderItemInfo, Text, View } from 'react-native';
import { FlatList } from 'react-native-gesture-handler';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { PublicKey } from 'core/objects';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { UserListItem } from '../components';
import { SocialHooks } from '../hooks';

/**
 * Component that will be used to allow users to search for other users or topics.
 * For now, it is used to show all the attendees of the last roll call
 */

const SocialSearch = () => {
  const currentLao = SocialHooks.useCurrentLao();

  if (!currentLao) {
    throw new Error('Impossible to open Social media Search if you are not connected to a LAO');
  }

  const rollCallId = currentLao.last_tokenized_roll_call_id;
  const attendees = SocialHooks.useRollCallAttendeesById(rollCallId);

  const renderAttendee = React.useCallback(
    ({ item: attendee, index: i }: ListRenderItemInfo<PublicKey>) => (
      <UserListItem
        publicKey={attendee}
        isFirstItem={i === 0}
        isLastItem={i === attendees.length - 1}
      />
    ),
    [attendees],
  );

  if (!rollCallId) {
    return (
      <ScreenWrapper>
        <Text style={Typography.base}>{STRINGS.social_media_user_list_unavailable}</Text>
      </ScreenWrapper>
    );
  }

  return (
    <ScreenWrapper>
      <View style={List.container}>
        <FlatList
          data={attendees}
          renderItem={renderAttendee}
          keyExtractor={(attendee) => attendee.toString()}
        />
      </View>
    </ScreenWrapper>
  );
};

export default SocialSearch;
