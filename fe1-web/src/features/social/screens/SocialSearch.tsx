import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import * as React from 'react';
import { FlatList, ListRenderItemInfo, StyleSheet, View, ViewStyle } from 'react-native';

import { TextBlock } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { SocialSearchParamList } from 'core/navigation/typing/SocialSearchParamList';
import { PublicKey } from 'core/objects';
import { gray } from 'core/styles/color';
import STRINGS from 'resources/strings';

import { UserListItem } from '../components';
import { SocialHooks } from '../hooks';
import { SocialFeature } from '../interface';

/**
 * Component that will be used to allow users to search for other users or topics.
 * For now, it is used to show all the attendees of the last roll call so that everyone can follow
 * whoever they want.
 */

const styles = StyleSheet.create({
  viewCenter: {
    alignSelf: 'center',
    width: 600,
  } as ViewStyle,
  titleTextView: {
    alignSelf: 'flex-start',
    marginTop: 20,
  } as ViewStyle,
  attendeesList: {
    flexDirection: 'column',
    marginTop: 20,
    borderTopWidth: 1,
    borderColor: gray,
  } as ViewStyle,
});

type NavigationProps = CompositeScreenProps<
  StackScreenProps<SocialSearchParamList, typeof STRINGS.social_media_navigation_tab_attendee_list>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_social_media>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

const SocialSearch = () => {
  const route = useRoute<NavigationProps['route']>();
  const { currentUserPublicKey } = route.params;
  const userPublicKey = new PublicKey(currentUserPublicKey);
  const currentLao = SocialHooks.useCurrentLao();

  if (!currentLao) {
    throw new Error('Impossible to open Social media Search if you are not connected to a LAO');
  }

  const rollCallId = currentLao.last_tokenized_roll_call_id;
  if (!rollCallId) {
    throw new Error(
      'Impossible to open social media search: last tokenized roll call id is undefined',
    );
  }
  const attendees = SocialHooks.useRollCallAttendeesById(rollCallId);

  const renderItem = ({ item }: ListRenderItemInfo<PublicKey>) => {
    // Not show our own profile
    if (item.valueOf() === currentUserPublicKey.valueOf()) {
      return null;
    }
    return (
      <UserListItem laoId={currentLao.id} publicKey={item} currentUserPublicKey={userPublicKey} />
    );
  };

  return (
    <ScreenWrapper>
      <View style={styles.viewCenter}>
        <View style={styles.titleTextView}>
          <TextBlock text={STRINGS.attendees_of_last_roll_call} />
        </View>
        <View style={styles.attendeesList}>
          <FlatList
            data={attendees}
            renderItem={renderItem}
            keyExtractor={(publicKey) => publicKey.valueOf()}
          />
        </View>
      </View>
    </ScreenWrapper>
  );
};

export default SocialSearch;

export const SocialSearchScreen: SocialFeature.SocialScreen = {
  id: STRINGS.social_media_navigation_tab_search,
  Component: SocialSearch,
};
