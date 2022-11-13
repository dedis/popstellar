import * as React from 'react';
import { useContext } from 'react';
import { FlatList, ListRenderItemInfo, StyleSheet, View, ViewStyle } from 'react-native';

import { TextBlock } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { PublicKey } from 'core/objects';
import { gray } from 'core/styles/color';
import STRINGS from 'resources/strings';

import { UserListItem } from '../components';
import { SocialMediaContext } from '../context';
import { SocialHooks } from '../hooks';

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

const SocialSearch = () => {
  const { currentUserPopTokenPublicKey } = useContext(SocialMediaContext);
  const currentLao = SocialHooks.useCurrentLao();

  if (!currentLao) {
    throw new Error('Impossible to open Social media Search if you are not connected to a LAO');
  }

  const rollCallId = currentLao.last_tokenized_roll_call_id;
  const attendees = SocialHooks.useRollCallAttendeesById(rollCallId);

  const renderItem = ({ item }: ListRenderItemInfo<PublicKey>) => {
    // Not show our own profile
    if (item.valueOf() === currentUserPopTokenPublicKey.valueOf()) {
      return null;
    }
    return <UserListItem laoId={currentLao.id} publicKey={item} />;
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
